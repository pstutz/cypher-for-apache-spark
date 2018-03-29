package org.opencypher

import java.io.File

import org.opencypher.GrammarExpr._
import org.opencypher.grammar._
import org.opencypher.okapi.trees.AbstractTreeNode
import org.opencypher.tools.grammar.Helpers._

object GrammarExpr {

  implicit class IdGeneration(a: Any) {
    def hashId: Int = {
      (a.hashCode & Int.MaxValue) % 100
    }
  }

  def generateRuleName(childRules: List[GrammarExpr]): String = {
    s"GeneratedAbstractClass${childRules.hashId}"
  }

}

abstract class GrammarExpr extends AbstractTreeNode[GrammarExpr] {
  def nameOpt: Option[String]

  def parameterName(implicit ruleMap: Map[String, Rule]): String = {
    nameOpt.getOrElse(scalaType.get.asParameter)
  }

  def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType]
}

case class Rule(name: String, parentClassOpt: Option[AbstractClassType], inline: Boolean, lexer: Boolean, definition: GrammarExpr) extends GrammarExpr {
  override def nameOpt: Option[String] = Some(name)

  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    if (lexer && name.isUpper) {
      None
    } else if (lexer) {
      Some(StringType)
    } else {
      definition match {
        case Either(sl, _) if sl.are[IgnoreCaseLiteral] => // Case class that wraps one string
          Some(CaseClassType(name, List(Parameter(name.asParamName, StringType)), parentClassOpt))
        case _: Either =>
          Some(AbstractClassType(name, parentClassOpt))
        case RuleRef(childName) if ruleMap(childName).scalaType.flatMap(_.superClass.map(_.name)).contains(name) =>
          // Child already extends parent, parent can be abstract
          Some(AbstractClassType(name, parentClassOpt))
        case _ => Some(CaseClassType(name, parameters, parentClassOpt))
      }
    }
  }

  def parameters(implicit ruleMap: Map[String, Rule]): List[Parameter] = {
    definition match {
      case _: Either => Nil
      case Sequence(exprs, _) =>
        val parametersWithTypes = exprs.flatMap(e => e.scalaType.map(e -> _))
        val params = parametersWithTypes.map { case (expr, typ) => Parameter(expr.parameterName, typ) }
        params
      case other => other.scalaType.map(Parameter(other.parameterName, _)).toList
    }
  }
}

abstract class TermExpr extends GrammarExpr

case class RuleRef(ruleName: String) extends TermExpr {
  override def nameOpt: Option[String] = Some(ruleName)

  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    ruleMap(ruleName).scalaType
  }
}

abstract class Literal extends TermExpr {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = Some(StringType)
}

case class StringLiteral(s: String) extends Literal {
  override def nameOpt: Option[String] = Some(s)
}

case class CharNotIn(nameOpt: Option[String], chars: String) extends Literal

case class CharIn(nameOpt: Option[String], chars: String) extends Literal

case class Fragment(nameOpt: Option[String], ps: Set[Int], namedInclusions: Set[String], namedExclusions: Set[String]) extends Literal {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = None
}

case class IgnoreCaseLiteral(nameOpt: Option[String], s: String) extends Literal {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = None
}


abstract class Repeat extends TermExpr {
  def expr: GrammarExpr

  def min: Int

  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = expr.scalaType.map(ListType)
}

case class RepeatWithSeparator(expr: TermExpr, sep: TermExpr, min: Int, maxOpt: Option[Int], nameOpt: Option[String]) extends Repeat

case class SimpleRepeat(expr: GrammarExpr, min: Int, maxOpt: Option[Int], nameOpt: Option[String] = None) extends Repeat

case class Optional(expr: GrammarExpr, nameOpt: Option[String] = None) extends TermExpr {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    expr.scalaType match {
      case l@Some(ListType(_)) => l
      case o@Some(OptionType(_)) => o
      case other => other.map(OptionType)
    }
  }
}

case class Either(exprs: List[GrammarExpr], nameOpt: Option[String] = None) extends TermExpr {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    val className = nameOpt.getOrElse {
      s"${
        nameOpt.getOrElse {
          s"GeneratedAbstractClass${exprs.hashId}"
        }
      }"
    }
    Some(AbstractClassType(className))
  }
}

case class Sequence(exprs: List[TermExpr], nameOpt: Option[String] = None) extends TermExpr {
  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
    val elementTypes = exprs.flatMap(_.scalaType)
    if (elementTypes.size == 1) {
      Some(elementTypes.head)
    } else if (elementTypes.isEmpty) {
      None
    } else {
      val name = generateRuleName(exprs)
      val parametersWithTypes = exprs.flatMap(e => e.scalaType.map(e -> _)).toMap
      val params = parametersWithTypes.map { case (expr, typ) => Parameter(expr.parameterName, typ) }.toList
      val c = CaseClassType(name, params, None)
      Some(c)
      //throw new Exception(s"Could not determine element type for list: elements ${exprs} with types $elementTypes")
    }
  }
}

object CypherGrammar {

  val oc = Grammar.OPENCYPHER_XML_NAMESPACE

  val cypherPath = new File("./okapi-frontend/grammar/cypher.xml").toPath

  def parserRules(): Map[String, Rule] = {
    val grammar: Grammar = Grammar.parseXML(cypherPath)
    var productions = Set.empty[Production]
    grammar.accept(new ProductionVisitor[Exception] {
      override def visitProduction(production: Production): Unit = {
        if (!production.legacy) productions += production
      }
    })
    val parsedRules: Map[String, Rule] = productions.map(p => p.name -> p.convert).toMap
    parsedRules
  }
}
