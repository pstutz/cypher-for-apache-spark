package org.opencypher.fastparse

import scala.language.higherKinds

object AstGenerator extends App {

  val rules: Map[String, Rule] = CypherGrammar.parserRules()
  implicit val helper = new ScalaTypeHelper(rules)

  import helper._

  rules.values.foreach { r =>
    println("\n============================================================\n")
    r.show()
    println
    println(r.definition.semanticContent.map(_.pretty))
    println
    println(r.returnType)
    println()
    println(r.returnType.typeSignature)
    r.returnType.scalaClassDef.foreach(println)
    println("\n============================================================\n")
  }

}

class ScalaTypeHelper(rules: Map[String, Rule]) {

  import StringUtilities._

  val keywordTerms: Set[IgnoreCaseLiteral] = rules("ReservedWord").collect { case l: IgnoreCaseLiteral => l }.toSet

  val keywords: Set[String] = keywordTerms.map(_.s.toUpperCase)

  implicit class GrammarExprConverter(expr: GrammarExpr) {

    def semanticContent(implicit insideMaybe: Boolean = false): Option[TermExpr] = {
      if (cachedSemanticContent.contains(expr -> insideMaybe)) {
        cachedSemanticContent(expr -> insideMaybe)
      } else {
        val content = expr match {
          case Rule("SP", _, _, _) => None
          case Rule("whitespace", _, _, _) => None
          case Rule(name, lexer, inline, definition) =>
            if (lexer) {
              definition.semanticContent.map(_ => RuleRef(name))
            } else {
              Some(RuleRef(name))
            }
          case RuleRef(refName) => rules(refName).semanticContent
          case _: StringLiteral => None
          case cni: CharNotIn => Some(cni)
          case ci: CharIn => Some(ci)
          case f: Fragment => Some(f)
          case i@IgnoreCaseLiteral(l) =>
            if (insideMaybe && keywords.contains(l.toUpperCase)) {
              Some(i)
            } else {
              None
            }
          case r: Rep =>
            val maybeContent = r.expr.semanticContent
            maybeContent.map(c => r.copy(expr = c))
          case r: RepSep =>
            val maybeContent = r.expr.semanticContent
            maybeContent match {
              case None => None
              case Some(c) => Some(r.copy(expr = c))
            }
          case Maybe(e) =>
            e.semanticContent(insideMaybe = true).map(Maybe(_))
          case Either(elements) =>
            val filtered = elements.flatMap(_.semanticContent)
            filtered match {
              case Seq() => None
              case Seq(one) => Some(one)
              case many => Some(Either(many))
            }
          case Sequence(elements) =>
            val filtered = elements.flatMap(_.semanticContent)
            filtered match {
              case Seq() => None
              case Seq(first, r@RepSep(repeated, _, 0, _)) if first == repeated =>
                Some(r.copy(min = 1))
              case Seq(first, r@Rep(repeated, 0, _)) if first == repeated =>
                Some(r.copy(min = 1))
              case Seq(one) => Some(one)
              case many => Some(Sequence(many))
            }
          case other => throw new UnsupportedOperationException(s"semanticContent cannot handle $other")
        }
        cachedSemanticContent += (expr, insideMaybe) -> content
        content
      }
    }

    def returnType: ScalaType = {
      expr match {
        case Rule(name, inline, lexer, definition) =>
          val paramName = name.asParamName
          if (lexer) {
            StringType(Some(paramName))
          } else {
            val semanticContent = definition.semanticContent
            semanticContent match {
              case None => StringType(Some(paramName))
              case Some(content) => content match {
                case RuleRef(refName) =>
                  CaseClassType(name, List(rules(refName).returnType), Some(paramName))
                case Either(_) =>
                  TraitType(name)
                case Sequence(parameters) =>
                  CaseClassType(name, parameters.map(_.returnType))
                case r: Repeat =>
                  val tp = r.returnType
                  if (inline) {
                    tp
                  } else {
                    CaseClassType(name, List(tp))
                  }
                case m: Maybe =>
                  val tp = m.returnType.withParameterName(s"maybe$name")
                  if (inline) {
                    tp
                  } else {
                    CaseClassType(name, List(tp), Some(paramName))
                  }
                case i: IgnoreCaseLiteral => i.returnType
                case other =>
                  println(s"TODO:")
                  expr.show()
                  StringType(Some(paramName))
              }
            }
          }
        //          }
        case RuleRef(refName) => rules(refName).returnType
        case Either(exprs) => TraitType(s"AnonymousTrait${exprs.mkString("Or")}")
        case Sequence(parameters) => TupleType(parameters.map(_.returnType))
        case r: Repeat => ListType(r.expr.returnType)
        case Maybe(parameter) =>
          val inner = parameter.returnType
          inner match {
            case b: BooleanType => b
            case _ => OptionType(inner)
          }
        case _: Fragment =>
          StringType()
        case IgnoreCaseLiteral(l) =>
          BooleanType(Some(l.toLowerCase))
        case _ =>
          println(s"TODO: $expr")
          StringType()
      }
    }

  }

  private var cachedSemanticContent: Map[(GrammarExpr, Boolean), Option[TermExpr]] = Map.empty

}
