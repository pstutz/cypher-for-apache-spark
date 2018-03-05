package org.opencypher.tools.grammar

import java.io.File

import org.opencypher.grammar.CharacterSet.DefinitionVisitor.NamedSetVisitor
import org.opencypher.grammar.CharacterSet.ExclusionVisitor
import org.opencypher.grammar.Grammar.Term
import org.opencypher.grammar.{CharacterSet, _}
import org.opencypher.okapi.trees.{AbstractTreeNode, BottomUp}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import org.opencypher.tools.grammar.CypherGrammar.RichString

trait ScalaType {
  def asParam: String = s"$paramName: $classAsString"

  def classAsString: String

  def paramName: String
}

case class CaseClassType(classAsString: String, params: ScalaType) extends ScalaType {
  override def paramName: String = classAsString.asParamName
}

case class AbstractClassType(classAsString: String) extends ScalaType {
  override def paramName: String = classAsString.asParamName
}

case class StringType(name: Option[String] = None) extends ScalaType {
  override def paramName: String = name.map(n => n.asParamName).getOrElse("s")

  override def classAsString: String = "String"
}

case class ListType(name: Option[String] = None, elementType: ScalaType) extends ScalaType {
  override def paramName: String = name.map(n => n.firstCharToLowerCase).getOrElse(s"${elementType.paramName.firstCharToLowerCase}s")

  override def classAsString: String = s"List[${elementType.classAsString}]"
}

case class ProductType(elementTypes: List[ScalaType]) extends ScalaType {

  override def paramName: String = {
    elementTypes match {
      case Nil => "p"
      case h :: _ => h.paramName
    }
  }

  override def asParam: String = elementTypes.map(_.asParam).mkString(", ")

  override def classAsString: String = {
    if (elementTypes.length == 1) {
      s"${elementTypes.head.classAsString}"
    } else {
      s"(${elementTypes.map(_.classAsString).mkString(", ")})"
    }
  }
}

case class OptionType(name: Option[String] = None, elementType: ScalaType) extends ScalaType {

  override def asParam: String = {
    elementType match {
      // Special cases to turn an optional literal into a boolean flag
      case StringType(Some(name)) => s"${name.asParamName}: Boolean"
      case ProductType(List(StringType(Some(name)))) => s"${name.asParamName}: Boolean"
      // Special case to get rid of complex options of lists
      case ProductType(List(head, tail: ListType)) if head.classAsString == tail.elementType.classAsString => s"${tail.asParam}"
      case _ => super.asParam
    }
  }

  override def paramName: String = name.map(n => n.firstCharToLowerCase).getOrElse(s"${elementType.paramName.firstCharToLowerCase}Option")

  override def classAsString: String = s"Option[${elementType.classAsString}]"
}

abstract class GrammarExpr extends AbstractTreeNode[GrammarExpr] {
  def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType]
}

case class Rule(name: String, inline: Boolean, lexer: Boolean, definition: TermExpr) extends GrammarExpr {

  def generateAstClass(implicit ruleMap: Map[String, Rule]): Option[String] = {
    scalaType match {
      case None => None
      case Some(_: StringType) => None
      case Some(defType) =>
        defType match {
          case AbstractClassType(className) =>
            Some(s"abstract class $className extends CypherExpr")
          case CaseClassType(_, _) =>
            definition.scalaType match {
              case Some(ProductType(List(head, tail: ListType))) if head.classAsString == tail.elementType.classAsString =>
                Some(s"case class ${defType.classAsString}(${tail.asParam}) extends CypherExpr")
              case _ =>
                val params = definition.scalaType(ruleMap).map(_.asParam)
                params.map(p => s"case class ${defType.classAsString}($p) extends CypherExpr")
            }
        }
    }
  }

  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    if (lexer && name.isUpper) {
      None
    } else if (lexer) {
      Some(StringType(Some(name)))
    } else {
      definition.scalaType match {
        case Some(t) => Some(CaseClassType(name, t))
        case None =>
          if (definition.isInstanceOf[Either] && definition.children.forall(_.isInstanceOf[RuleRef])) {
            Some(AbstractClassType(name))
          } else {
            None
          }
      }
    }
  }
}

abstract class TermExpr extends GrammarExpr

case class RuleRef(ruleName: String) extends TermExpr {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    val rule = ruleMap(ruleName)
    rule.scalaType
  }
}

abstract class Literal extends TermExpr {
  def length: Int

  def possibleCharacters: String
}

case class StringLiteral(s: String) extends Literal {
  override def length: Int = s.length

  override def possibleCharacters = s

  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = None
}

case class CharNotIn(name: Option[String], chars: String) extends Literal {
  def length: Int = 1

  def possibleCharacters = throw new UnsupportedOperationException("Possible characters on AnyCharExcept")

  override def toString = "AnyChar"

  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = Some(StringType(name))
}

case class CharIn(name: Option[String], chars: String) extends Literal {
  override def length: Int = 1

  override def possibleCharacters = chars.mkString

  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = Some(StringType(name))
}

case class Fragment(name: Option[String], ps: Set[Int], namedInclusions: Set[String], namedExclusions: Set[String])
  extends Literal {
  override def length: Int = 1

  override def possibleCharacters = ps.map(_.toChar).mkString

  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    name match {
      case Some(n) if (n.isUpper) => None
      case _ => Some(StringType(name))
    }
  }
}

case class IgnoreCaseLiteral(name: Option[String], s: String) extends Literal {
  override def length: Int = s.length

  override def possibleCharacters = s.toLowerCase + s.toUpperCase

  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    if (s.isKeyword) {
      name match {
        case None => Some(StringType(Some(s)))
        case _ => Some(StringType(name))
      }
    } else {
      None
    }
  }
}

case class Repeat(name: Option[String], expr: TermExpr, min: Int, max: Option[Int]) extends TermExpr {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    expr.scalaType match {
      case Some(s: StringType) => Some(s)
      case Some(e) => Some(ListType(name, e))
      case None => None
    }
  }
}

case class Optional(name: Option[String], expr: TermExpr) extends TermExpr {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    expr.scalaType match {
      case None => None
      case Some(t) => Some(OptionType(name, t))
    }
  }
}

case class Either(exprs: List[TermExpr]) extends TermExpr {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    None
  }
}

case class Sequence(exprs: List[TermExpr]) extends TermExpr {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    val elementTypes = exprs.filter(e => !(e.isInstanceOf[IgnoreCaseLiteral] && e.asInstanceOf[IgnoreCaseLiteral].s.isKeyword)).flatMap(_.scalaType)
    if (elementTypes.length == 1) {
      Some(elementTypes.head)
    } else if (elementTypes.length > 1) {
      Some(ProductType(elementTypes))
    } else {
      None
    }
  }
}

object CypherGrammar extends App {

  val oc = Grammar.OPENCYPHER_XML_NAMESPACE

  val cypherPath = new File("./okapi-frontend/grammar/cypher.xml").toPath


  val grammar: Grammar = Grammar.parseXML(cypherPath)

  var productions = Set.empty[Production]

  grammar.accept(new ProductionVisitor[Exception] {
    override def visitProduction(production: Production): Unit = {
      if (!production.legacy) productions += production
    }
  })

  val parsedTrees = productions.map(p => p.name -> p.convert).toMap

  val inlineEitherWithoutChoice: BottomUp[GrammarExpr] = BottomUp {
    case r@Rule(_, _, _, Either(List(single))) =>
      r.copy(definition = single)
  }

  val withInlinedSimpleEither = parsedTrees.mapValues(inlineEitherWithoutChoice.rewrite(_).asInstanceOf[Rule])

  //  val aliases = withInlinedSimpleEither.values.foldLeft(Map.empty[String, String]) { case (a, n) =>
  //    n match {
  //      case Rule(aliasName, _, _, RuleRef(replacement)) => a.updated(aliasName, replacement)
  //      case _ => a
  //    }
  //  }
  //
  //  println(aliases)
  //
  //  val withReplacedAliases: Map[String, Rule] = withInlinedSimpleEither.mapValues { inlined: Rule =>
  //    inlined.map { node: GrammarExpr =>
  //      node match {
  //        case RuleRef(alias) if aliases.keySet.contains(alias) =>
  //          println(s"Substituting '${alias}' with '${aliases(alias)}'")
  //          RuleRef(aliases(alias))
  //        case _ => node
  //      }
  //    }.asInstanceOf[Rule]
  //  }


  implicit val trees: Map[String, Rule] = withInlinedSimpleEither

  val rootRule = trees("Cypher")

  recurse(rootRule, 9)

  def recurse(r: Rule, depth: Int = 1, visited: Set[String] = Set.empty): Unit = {
    if (depth > 0) {
      debug(r)
      if (depth > 1) {
        val subRules = r.definition.collect { case RuleRef(name) => name }.toSet -- visited
        subRules.foreach { sr =>
          if (sr.scalaType.isDefined || sr.definition.isInstanceOf[Either]) {
            recurse(trees(sr.name), depth - 1, visited + sr.name)
          }
          //          else {
          //            println(s"Stopped at ${sr.name}")
          //          }
        }
      }
    }
  }

  def debug(t: Rule): Unit = {
    println("\n=======================================================================================")
    println(t.pretty)

    t.scalaType.map(println)
    val astClass = t.generateAstClass
    astClass match {
      case Some(s) =>
        println(s)
      case None =>
    }
    println("=======================================================================================\n")
  }

  //  trees.values.foreach { t =>
  //    println("\n=======================================================================================\n")
  //    println(t.pretty)
  //
  //    val astClass = t.generateAstClass
  //    astClass match {
  //      case Some(s) =>
  //        println("\n=======================================================================================\n")
  //        println(s)
  //      case None =>
  //        t.scalaType.map(println)
  //    }
  //  }


  //val optimizeFragments = ???

  //  val optimizeCharIn: BottomUp[GrammarExpr] = BottomUp {
  //    case Either(exprs) if exprs.exists(l => !l.isInstanceOf[CharNotIn] && l.isInstanceOf[Literal] && l.asInstanceOf[Literal].length == 1) =>
  //      val (toTransform, toKeep) = exprs.partition(e => !e.isInstanceOf[CharNotIn] && e.isInstanceOf[Literal] && e.asInstanceOf[Literal].length == 1)
  //      val transformed = CharIn(toTransform.map(t => t.asInstanceOf[Literal].possibleCharacters).mkString.distinct.toCharArray.sorted.mkString)
  //      if (toKeep.isEmpty) {
  //        transformed
  //      } else {
  //        Either(transformed :: toKeep)
  //      }
  //  }

  //  val rewritten = trees.mapValues(optimizeCharIn.rewrite)
  //
  //  def shouldInline(e: GrammarExpr): Boolean = {
  //    e match {
  //      case RuleRef(ruleName) =>
  //      case _ => false
  //    }
  //  }

  //  abstract class ScalaAstExpression extends AbstractTreeNode[ScalaAstExpression]
  //
  //  case class AbstractScalaAstClass(name: String, isAbstract: Boolean)
  //
  //  case class ScalaAstClass(name: String, parameters: List[ScalaAstExpression])
  //
  //  case class RuleParameter(name: String) extends ScalaAstExpression
  //
  //  val rewriteToScalaAst: BottomUp[GrammarExpr] = BottomUp {
  //    case Rule(name, false, _, Either(exprs)) if exprs.are[RuleRef] && exprs.have[RuleRef](_.in) =>
  //      val
  //      AbstractScalaAstClass(name, true)
  //  }

  implicit class RichString(s: String) {
    def isUpper: Boolean = s.toCharArray.forall(_.isUpper)

    def isKeyword: Boolean = s.isUpper && s.toCharArray.forall(_.isLetter)

    def firstCharToLowerCase: String = {
      if (s.length == 0) {
        s
      } else {
        val c = s.toCharArray
        c(0) = Character.toLowerCase(c(0))
        new String(c)
      }
    }

    def asParamName: String = {
      if (isUpper) {
        s.toLowerCase
      } else {
        firstCharToLowerCase
      }
    }

  }

  implicit class RichList(val l: List[_]) extends AnyVal {

    def have[C: ClassTag](f: C => Boolean): Boolean = {
      are[C] && as[C].forall(f)
    }

    def are[C: ClassTag]: Boolean = {
      l.forall {
        case _: C => true
        case _ => false
      }
    }

    def as[C]: List[C] = {
      l.asInstanceOf[List[C]]
    }

  }


  implicit class RichProduction(p: Production) {
    def convert: Rule = {
      Rule(p.name, p.inline, p.lexer, p.definition.convert)
    }
  }

  implicit class RichTerm(t: Term) {
    def convert: TermExpr = {
      case class TermBuilder() extends TermVisitor[RuntimeException] {

        def result = {
          assert(_result != null)
          _result
        }

        private var _result: TermExpr = _

        override def visitAlternatives(alternatives: Alternatives): Unit = {
          _result = Either(alternatives.asScala.toList.map(_.convert))
        }

        override def visitEpsilon(): Unit = {}

        override def visitSequence(sequence: org.opencypher.grammar.Sequence): Unit = {
          _result = Sequence(sequence.asScala.toList.map(_.convert))
        }

        override def visitNonTerminal(nonTerminal: NonTerminal): Unit = {
          _result = RuleRef(nonTerminal.productionName)
        }

        override def visitOptional(optional: org.opencypher.grammar.Optional): Unit = {
          _result = Optional(None, optional.term.convert)
        }

        override def visitCharacters(characters: CharacterSet): Unit = {
          _result = characters.convert
        }

        override def visitLiteral(literal: org.opencypher.grammar.Literal): Unit = {
          _result = literal.convert
        }

        override def visitRepetition(repetition: Repetition): Unit = {
          val max = if (repetition.limited) Some(repetition.maxTimes) else None
          _result = Repeat(None, repetition.term.convert, repetition.minTimes, max)
        }
      }

      // Hide mutable visitor pattern
      val builder = TermBuilder()
      t.accept(builder)
      builder.result
    }

    implicit class RichCharacterSet(cs: CharacterSet) {
      def convert: TermExpr = {
        case class CharacterSetBuilder() extends NamedSetVisitor[RuntimeException] with ExclusionVisitor[RuntimeException] {

          def result = {
            Fragment(Option(cs.name), codePoints -- exclusions, namedInclusions, namedExclusions)
          }

          var codePoints = Set.empty[Int]
          var exclusions = Set.empty[Int]
          var namedInclusions: Set[String] = Set.empty
          var namedExclusions: Set[String] = Set.empty

          override def visitCodePoint(cp: Int): Unit = codePoints += cp

          override def excludeCodePoint(cp: Int): Unit = exclusions -= cp

          override def visitSet(name: String): ExclusionVisitor[RuntimeException] = {
            namedInclusions += name
            this
          }

          override def excludeSet(name: String): Unit = {
            namedExclusions += name
          }
        }

        // Hide mutable visitor pattern
        val builder = CharacterSetBuilder()
        cs.accept(builder)
        builder.result
      }
    }

  }

  implicit class RichLiteral(l: org.opencypher.grammar.Literal) {
    def convert: Literal = {
      if (l.caseSensitive) StringLiteral(l.toString) else IgnoreCaseLiteral(None, l.toString)
    }
  }

}
