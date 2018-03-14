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
  val rewrittenRules: Map[String, Rule] = {
    val r = repSep(rules)
    rules.mapValues { rule =>
      r.rewrite(rule) match {
        case stillRule: Rule => stillRule
        case _ => throw new Exception("rewritten rule needs to be a rule")
      }
    }
  }

  val eitherExpressions = rewrittenRules.values.foldLeft(List.empty[(GrammarExpr, List[GrammarExpr])]) { case (eithers, rule) =>
    rule.foldLeft(eithers) { case (eithersInner, expr) => expr match {
      case e: GrammarExpr => if e.children.exists(_.isInstanceOf[Either]) =>
    eithersInnter ++ e -> e.children.filter (_.isInstanceOf[Either] )
      case _ => eithersInner
    }
    }
  }

  val rootExpression = AbstractClassType("CypherExpr", AbstractClassType("AbstractTreeNode[CypherExpr]"))

  // Repeat with separator
  def repSep(implicit ruleMap: Map[String, Rule]) = BottomUp[GrammarExpr] {
    case Sequence(List(a, Repeat(Sequence(List(sep, aAgain), innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
      if a == aAgain && sep.scalaType == None =>
      val nameOpt: Option[String] = List(seqNameOpt, repNameOpt, innerSeqName, a.scalaType.map(t => (t.toString + "s").asParamName)).flatten.headOption
      RepeatWithSeparator(a, sep, min + 1, maxOpt.map(_ + 1), nameOpt)
  }

  def transform(ruleName: String): Unit = {
    val rule = rewrittenRules(ruleName)

    println(rule.definition.pretty)
    val asScala = rule.scalaType(rewrittenRules)
    println(asScala)
    println(rule.asScalaClass(rewrittenRules).get)
    //???
  }

  //rules.values.filter(_.lexer).foreach(r => println(r.name))

  //transform("WHERE")
  //Parameter
  //transform("NodeLabels")

  transform("Parameter")

  //transform("Cypher")

  //oC_NodeLabels : oC_NodeLabel ( SP? oC_NodeLabel )* ;"

  implicit class ParametersForRule(val r: Rule) extends AnyVal {
    def asScalaClass(implicit ruleMap: Map[String, Rule]): Option[String] = {
      r.scalaType match {
        case Some(CaseClassType(name, parameters, superClassOption)) =>
          Some(s"case class $name(${parameters.mkString(", ")})${superClassOption.map(s => s" extends $s").getOrElse("")}")
        case Some(AbstractClassType(name, superClass)) =>
          Some(s"abstract class $name extends $superClass")
        case other => throw new Exception(s"Could not turn $r into a scala class")
      }
    }

    def parameters(implicit ruleMap: Map[String, Rule]): List[Parameter] = {
      r.definition match {
        case _: Either => Nil
        case Sequence(exprs, _) =>
          val parametersWithTypes = exprs.flatMap(e => e.scalaType.map(e -> _)).toMap
          parametersWithTypes.map { case (expr, typ) => Parameter(expr.parameterName, typ) }.toList
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
        case Optional(expr, nameOpt) => nameOpt.map(_.asParamName).getOrElse("option")
        case Either(_, nameOpt) => nameOpt.map(_.asParamName).getOrElse("anonymousEither")
        case Sequence(exprs, nameOpt) => nameOpt.map(_.asParamName).getOrElse("sequence")
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
        case Optional(expr, _) => expr.scalaType.map(OptionType)
        case Either(_, nameOpt) => Some(AbstractClassType(nameOpt.getOrElse(s"AnonymousAbstractClass#${anonCounter.incrementAndGet()}")))
        case Sequence(exprs, _) =>
          val elementTypes = exprs.flatMap(_.scalaType).toSet
          if (elementTypes.size == 1) {
            Some(ListType(elementTypes.head))
          } else if (elementTypes.size == 0) {
            None
          } else {
            throw new Exception(s"Could not determine element type for list: elements ${exprs} with types $elementTypes")
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
