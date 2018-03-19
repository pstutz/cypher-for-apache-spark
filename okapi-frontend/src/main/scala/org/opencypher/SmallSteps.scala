package org.opencypher

import java.util.concurrent.atomic.AtomicInteger

import org.opencypher.okapi.trees.{AbstractTreeNode, BottomUp, TopDown}
import org.opencypher.tools.grammar.Helpers._
import org.opencypher.tools.grammar._

// Use only the simplest atomic parsing concepts
// Rewrite later on to make more efficient

object SmallSteps extends App {
  val anonCounter = new AtomicInteger()

  val rules: Map[String, Rule] = CypherGrammar.parserRules()

  //case class RelationshipTypes(relTypeName: RelTypeName, list: List[RelTypeName]) extends CypherExpression

  //println(usages)
  //  traverse(0, "Cypher")
  //  var maxDepth = 0
  //
  //  def traverse(depth: Int = 0, ruleName: String, encounteredBefore: Set[String] = Set.empty): Unit = {
  //    if (depth > maxDepth) {
  //      println(s"$maxDepth with ${encounteredBefore.size} entries")
  //      maxDepth = math.max(maxDepth, depth)
  //    }
  //    rules(ruleName).foreach { c: GrammarExpr =>
  //      c match {
  //        case RuleRef(n) =>
  //          if (encounteredBefore.contains(n)) {
  //            throw new Exception(s"Cycle detected: $ruleName => $n")
  //          }
  //          traverse(depth + 1, n, encounteredBefore + ruleName)
  //        case _ =>
  //      }
  //    }
  //  }

  //  var rewritten = Set.empty[GrammarExpr]
  //
  //  val assembleParseTree = TopDown[GrammarExpr] {
  //    case r@RuleRef(ruleName) =>
  //      val rule = rules(ruleName)
  //      if (rewritten.contains(rule)) {
  //        println(s"already encountered:\n${rule.pretty}")
  //        r
  //      } else {
  //        rewritten += rule
  //        rule
  //      }
  //  }
  ////  val root = assembleParseTree.rewrite(rules("Cypher"))
  //
  //  println(root.pretty)


  //TODO: Cover all cases elegantly: Arbitrary many elements before first, and arbitrarily many elements of type None as separators
  // Repeat with separator
  def repSep(implicit ruleMap: Map[String, Rule]) = BottomUp[GrammarExpr] {
    //    case Sequence(List(a, Repeat(Sequence(List(aAgain), innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
    //      if a.scalaType(ruleMap) == aAgain.scalaType(ruleMap) =>
    //      val nameOpt: Option[String] = List(seqNameOpt, repNameOpt, innerSeqName, a.scalaType.map(t => (t.toString + "s").asParamName)).flatten.headOption
    //      Repeat(a, min + 1, maxOpt.map(_ + 1), nameOpt)
    //    case Sequence(List(a, Repeat(Sequence(List(sep, aAgain), innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
    //      if a.scalaType(ruleMap) == aAgain.scalaType(ruleMap) && sep.scalaType == None =>
    //      val nameOpt: Option[String] = List(seqNameOpt, repNameOpt, innerSeqName, a.scalaType.map(t => (t.toString + "s").asParamName)).flatten.headOption
    //      RepeatWithSeparator(a, sep, min + 1, maxOpt.map(_ + 1), nameOpt)
    case Sequence(List(a, Repeat(Sequence(inner, innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
      if a == inner.last && inner.dropRight(1).forall(_.scalaType.isEmpty) =>
      val nameOpt: Option[String] = List(seqNameOpt, repNameOpt, innerSeqName, a.scalaType.map(t => (t.toString + "s").asParamName)).flatten.headOption
      RepeatWithSeparator(a, Sequence(inner.dropRight(1)), min + 1, maxOpt.map(_ + 1), nameOpt)

    // TODO: Create rewriting facilities to express this concisely
    case Sequence(List(p1, a, Repeat(Sequence(inner, innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
      if a == inner.last && inner.dropRight(1).forall(_.scalaType.isEmpty) =>
      val nameOpt: Option[String] = List(seqNameOpt, repNameOpt, innerSeqName, a.scalaType.map(t => (t.toString + "s").asParamName)).flatten.headOption
      Sequence(List(p1, RepeatWithSeparator(a, Sequence(inner.dropRight(1)), min + 1, maxOpt.map(_ + 1), nameOpt)), seqNameOpt)

    // TODO: Create rewriting facilities to express this concisely
    case Sequence(List(p1, p2, a, Repeat(Sequence(inner, innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
      if a == inner.last && inner.dropRight(1).forall(_.scalaType.isEmpty) =>
      val nameOpt: Option[String] = List(seqNameOpt, repNameOpt, innerSeqName, a.scalaType.map(t => (t.toString + "s").asParamName)).flatten.headOption
      Sequence(List(p1, p2, RepeatWithSeparator(a, Sequence(inner.dropRight(1)), min + 1, maxOpt.map(_ + 1), nameOpt)), seqNameOpt)

    // TODO: Create rewriting facilities to express this concisely
    case Sequence(List(p1, p2, p3, a, Repeat(Sequence(inner, innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
      if a == inner.last && inner.dropRight(1).forall(_.scalaType.isEmpty) =>
      val nameOpt: Option[String] = List(seqNameOpt, repNameOpt, innerSeqName, a.scalaType.map(t => (t.toString + "s").asParamName)).flatten.headOption
      Sequence(List(p1, p2, p3, RepeatWithSeparator(a, Sequence(inner.dropRight(1)), min + 1, maxOpt.map(_ + 1), nameOpt)), seqNameOpt)

  }

  val rewrittenRules: Map[String, Rule] = {
    val r = repSep(rules)
    rules.mapValues { rule =>
      r.rewrite(rule) match {
        case stillRule: Rule => stillRule
        case _ => throw new Exception("rewritten rule needs to be a rule")
      }
    }
  }

  //val root = rewrittenRules("Cypher")

  // Infer common supertype for multiple rules by walking up the usage tree

  // For each rule that has a ScalaType, find out in which other rules it is used.
  // If there are several, check if one is a valid instance of the other.
  // Determine if it ever happens that this is not the case.
  //
  //  val rulesWithScalaType = rules.map { case (n, r) => n -> r.scalaType(rules) }.foreach {
  //    case (n, t) => t match {
  //      case None => println(s"$n has no scala type") // Only SP has no Scala type
  //      case Some(_) =>
  //    }
  //  }

  def computeUsages(ruleName: String, r: Map[String, Rule]): (String, Set[String]) = {
    val usedBy = r.foldLeft(Set.empty[String]) { case (u, (n2, r2)) =>
      if (r2.exists(_ == RuleRef(ruleName))) {
        u + n2
      } else {
        u
      }
    }
    ruleName -> usedBy
  }

  val usages = rewrittenRules.map { case (n, r) =>
    computeUsages(n, rewrittenRules)
  }

  val rootExpressionType = AbstractClassType("CypherExpression", Some(AbstractClassType("AbstractTreeNode[CypherExpression]")))

  def parentType(ruleName: String, rootType: AbstractClassType = rootExpressionType): Option[AbstractClassType] = {
    val rule = rewrittenRules(ruleName)
    val st = rule.scalaType(rewrittenRules)
    st match {
      case None => None
      case Some(StringType) => None
      case Some(other) =>
        val us: Set[String] = usages(ruleName)
        if (us.isEmpty) {
          Some(rootType)
        } else if (us.size == 1) {
          val parentRuleName = us.head
          val parentRule: Rule = rewrittenRules(parentRuleName)
          val parentParams = parentRule.parameters(rewrittenRules)
          parentParams match {
            case List() => Some(AbstractClassType(parentRuleName))
            case List(_) => Some(AbstractClassType(parentRuleName))
            case List(Parameter(_, a), Parameter(_, ListType(b))) =>
              if (a == b) {
                parentType(parentRuleName)
              } else {
                Some(rootType)
              }
            case _ => Some(rootType)
          }
          //        } else if (us.size == 2) {
          //          val p1 = parentType(us.head)
          //          val p2 = parentType(us.drop(1).head)
          //          if (p1 == p2) {
          //            p1
          //          } else {
          //            Some(rootType)
          //          }
        } else {
          Some(rootType)
        }
    }
  }

  val rulesWithParentTypes = rewrittenRules.map { case (n, r) =>
    val pt = parentType(n)
    n -> r.copy(parentClassOpt = pt)
  }

  val classDefs = {
    rulesWithParentTypes.flatMap { case (_, r) =>
      r.asScalaClass(rulesWithParentTypes)
    }
  }.mkString("\n")

  println(classDefs)

//  val r = rulesWithParentTypes("AndExpression")
//  println(r.pretty)


  var nextId = 0

  def generateId: Int = {
    nextId += 1
    nextId
  }

  def generateRuleName(childRules: List[GrammarExpr]): String = {
    s"GeneratedRule$generateId"
  }

  //  val eitherRules = rewrittenRules.values.foldLeft(List.empty[(GrammarExpr, List[GrammarExpr])]) { case (eithers, rule) =>
  //    rule.foldLeft(eithers) { case (eithersInner, expr) => expr match {
  //      case e: GrammarExpr if e.children.hasA[Either] =>
  //        val newRulesForEithers = e.children.mapOnly()
  //        ???
  //      case _ => eithersInner
  //    }
  //    }
  //  }


  //    rule.foldLeft(eithers) { case (eithersInner, expr) => expr match {
  //      case e: GrammarExpr => if e.children.exists(_.isInstanceOf[Either]) =>
  //    eithersInner ++ e -> e.children.filter (_.isInstanceOf[Either] )
  //      case _ => eithersInner
  //    }
  //    }
  //  }

  //  def transform(ruleName: String): Unit = {
  //    val rule = rewrittenRules(ruleName)
  //
  //    println(rule.definition.pretty)
  //    val asScala = rule.scalaType(rewrittenRules)
  //    println(asScala)
  //    println(rule.asScalaClass(rewrittenRules).get)
  //    //???
  //  }

  //rules.values.filter(_.lexer).foreach(r => println(r.name))

  //transform("WHERE")
  //Parameter
  //transform("NodeLabels")

  //  transform("Parameter")

  //transform("Cypher")

  //oC_NodeLabels : oC_NodeLabel ( SP? oC_NodeLabel )* ;"

  implicit class ParametersForRule(val r: Rule) extends AnyVal {
    def asScalaClass(implicit ruleMap: Map[String, Rule], rootClass: AbstractClassType = rootExpressionType): Option[String] = {
      r.scalaType match {
        case Some(CaseClassType(name, parameters, superClassOption)) =>
          Some(s"case class $name(${parameters.mkString(", ")}) extends ${superClassOption.getOrElse(rootClass)}")
        case Some(AbstractClassType(name, superClassOption)) =>
          Some(s"abstract class $name extends ${superClassOption.getOrElse(rootClass)}")
        case _ =>
          None
        //throw new Exception(s"Could not turn $r into a scala class")
      }
    }

    def parameters(implicit ruleMap: Map[String, Rule]): List[Parameter] = {
      r.definition match {
        case _: Either => Nil
        case Sequence(exprs, _) =>
          val parametersWithTypes = exprs.flatMap(e => e.scalaType.map(e -> _)).toMap
          val params = parametersWithTypes.map { case (expr, typ) => Parameter(expr.parameterName, typ) }.toList
          params
        case other => other.scalaType.map(Parameter(other.parameterName, _)).toList
      }
    }
  }

  implicit class GrammarToScala(val expr: GrammarExpr) extends AnyVal {

    def parameterName(implicit ruleMap: Map[String, Rule]): String = {
      expr match {
        case Rule(name, _, _, _, _) => name.asParamName
        case RuleRef(ruleName) => ruleName.asParamName
        case l: Literal => l.getClass.getSimpleName.asParamName
        case RepeatWithSeparator(expr, _, _, _, nameOpt) => nameOpt.map(_.asParamName).getOrElse("list")
        case Repeat(expr, _, _, nameOpt) => nameOpt.map(_.asParamName).getOrElse("list")
        case Optional(expr, nameOpt) => nameOpt.map(_.asParamName).getOrElse {
          expr.parameterName + "Opt"
        }
        case Either(_, nameOpt) => nameOpt.map(_.asParamName).getOrElse("anonymousEither")
        case Sequence(exprs, nameOpt) => nameOpt.map(_.asParamName).getOrElse {
          exprs match {
            case List(e) => e.parameterName
            case l if l.flatMap(_.scalaType).toSet.size == 1 =>
              val name = l.flatMap(_.scalaType).toSet.head.toString.asParamName
              if (name.endsWith("s") || l.flatMap(_.scalaType).size == 1) {
                name
              } else {
                name + "s"
              }
            case _ => "sequence"
          }
        }
      }
    }

    def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
      expr match {
        case r@Rule(name, parentClassOpt, inline, lexer, definition) =>
          if (lexer && name.isUpper) {
            None
          } else if (lexer) {
            Some(StringType)
          } else {
            definition match {
              case _: Either => Some(AbstractClassType(name, parentClassOpt))
              case _ => Some(CaseClassType(name, r.parameters, parentClassOpt))
            }
          }
        case RuleRef(ruleName) => ruleMap(ruleName).scalaType
        case _: IgnoreCaseLiteral => None
        case Fragment(Some(fName), _, _, _) if fName.isUpper => None
        case _: Literal => Some(StringType)
        case Repeat(expr, _, _, _) => expr.scalaType.map(ListType)
        case RepeatWithSeparator(expr, _, _, _, _) => expr.scalaType.map(ListType)
        case Optional(expr, _) => expr.scalaType match {
          case l@Some(ListType(_)) => l
          case other => other.map(OptionType)
        }
        case Either(_, nameOpt) => Some(AbstractClassType(nameOpt.getOrElse(s"AnonymousAbstractClass#${anonCounter.incrementAndGet()}")))
        case Sequence(exprs, _) =>
          val elementTypes = exprs.flatMap(_.scalaType).toSet
          if (elementTypes.size == 1) {
            Some(elementTypes.head)
          } else if (elementTypes.size == 0) {
            None
          } else {
            val name = generateRuleName(exprs)
            //TODO: Reuse parameter calculation code
            val parametersWithTypes = exprs.flatMap(e => e.scalaType.map(e -> _)).toMap
            val params = parametersWithTypes.map { case (expr, typ) => Parameter(expr.parameterName, typ) }.toList
            val c = CaseClassType(name, params, None)
            Some(c)
            //throw new Exception(s"Could not determine element type for list: elements ${exprs} with types $elementTypes")
          }
      }
    }
  }

}

//abstract class Parser extends AbstractTreeNode[Parser] {
//  def parser: String
//
//
//  def outputType: ScalaType
//}

case class Parameter(name: String, typ: ScalaType) {
  override def toString = s"$name: $typ"
}

abstract class ScalaType extends AbstractTreeNode[ScalaType] {
  def name: String

  override def toString = name
}

case object StringType extends ScalaType {
  def name = "String"
}

object AbstractClassType {
  def apply(name: String, superClass: AbstractClassType) = new AbstractClassType(name, Some(superClass))
}

case class AbstractClassType(name: String, superClass: Option[AbstractClassType] = None) extends ScalaType

case class CaseClassType(name: String, parameters: List[Parameter], superClass: Option[AbstractClassType]) extends ScalaType

case class ListType(elementType: ScalaType) extends ScalaType {
  override def name: String = s"List[$elementType]"
}

case class OptionType(elementType: ScalaType) extends ScalaType {
  override def name: String = s"Option[$elementType]"
}

//
//abstract class Parser extends AbstractTreeNode[Parser]
//
//case class StringInIgnoreCaseParser(parameter: String) extends Parser {
//  override def toString = s"""StringInIgnoreCase("$parameter")"""
//}
