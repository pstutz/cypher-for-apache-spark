package org.opencypher.v3

import scala.collection.immutable.Set
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
    println(r.parserString)
    println()
    println(r.returnType.typeSignature)
    val parseMethod = s"def parser: P[${r.returnType.typeSignature}] = P { ${r.parserString} }"
    r.returnType.scalaClassDef(r.parentTraits, List(parseMethod)).foreach(println)
    println("\n============================================================\n")
  }

}

class ScalaTypeHelper(rules: Map[String, Rule]) {

  import StringUtilities._

  val keywordTerms: Set[IgnoreCaseLiteral] = rules("ReservedWord").collect { case l: IgnoreCaseLiteral => l }.toSet

  val keywords: Set[String] = keywordTerms.map(_.s.toUpperCase)

  implicit class GrammarExprConverter(expr: GrammarExpr) {

    def parserString: String = {
      val s = expr match {
        case r@Rule(name, _, _, definition) =>
          val st = r.returnType
          st match {
            case TraitType(_, _) => s"${definition.parserString}"
            case _ =>
              val maybeMultipleParams = definition.semanticContent
              maybeMultipleParams match {
                case Some(Sequence(_)) =>
                  s"(${definition.parserString}).map($name)"
                case _ => s"${definition.parserString}.map(p => $name(p))"
              }
          }

        case RuleRef(refName) =>
          s"$refName.parser"
        //          val r = rules(refName)
        //          val signature = r.returnType
        //          signature match {
        //            case StringType(_) => s"${r.definition.parserString}"
        //            case _ => s"$refName.parser"
        //          }

        case StringLiteral(s) => s""""$s""""

//        case Fragment(ps, _, _) => // TODO: Handle named sets
//          val unicodeChars = ps.map(codePoint => Character.toString(codePoint.toChar)).mkString
//          s"""CharIn("$unicodeChars")"""

        case CharIn(cs, _) =>
          s"""CharIn("$cs")"""

        case IgnoreCaseLiteral(s, _) =>
          s"""IgnoreCase("$s")"""

        case Rep(rep, min, maybeMax) =>
          val minParserString = if (min > 0) Some(s"min = $min") else None
          val maxParserString = maybeMax.map(m => s"max = $m")
          val repArgs = List(minParserString, maxParserString).flatten.mkString(", ")
          if (repArgs == "") {
            s"(${rep.parserString}).rep"
          } else {
            s"(${rep.parserString}).rep($repArgs)"
          }

        case RepSep(rep, sep, min, maybeMax) =>
          val sepString = Some(s"sep = (${sep.parserString})")
          val minParserString = if (min > 0) Some(s"min = $min") else None
          val maxParserString = maybeMax.map(m => s"max = $m")
          val repArgs = List(sepString, minParserString, maxParserString).flatten.mkString(", ")
          s"(${rep.parserString}).rep($repArgs)"

        case Maybe(inner) =>
          s"(${inner.parserString}).?"

        case Either(alternatives) =>
          s"(${alternatives.map(a => s"(${a.parserString})").mkString(" | ")})"

        case Sequence(elements) =>
          s"(${elements.map(e => s"(${e.parserString})").mkString(" ~ ")})"

        case other => s"TODO parserString $other"
      }

      //      expr.semanticContent match {
      //        case r: Rule =>
      //      }
      if (!expr.isInstanceOf[Rule] && expr.semanticContent.isDefined) {
        s"$s.!"
      } else {
        s
      }

      //      } else {
      //        s
      //      }
    }

    def parentTraits: List[String] = {
      if (cachedParentTraits.contains(expr)) {
        cachedParentTraits(expr)
      } else {
        val pt = expr match {
          case Rule(name, _, _, _) => RuleRef(name).parentTraits
          case _ =>
            rules.values.flatMap {
              rule =>
                //                if (!expr.isInstanceOf[RuleRef] || expr.asInstanceOf[RuleRef].ruleName != rule.name) {
                val rt = rule.returnType
                rt match {
                  case _: TraitType =>
                    val content = rule.definition.semanticContent
                    content match {
                      case Some(e) if expr != RuleRef(rule.name) && expr == e => Some(rule.name)
                      case Some(Either(exprs)) if exprs.contains(expr) => Some(rule.name)
                      case _ => None
                    }
                  case _ => None
                }
              //                } else {
              //                  None
              //                }
            }.toList
        }
        cachedParentTraits += expr -> pt
        pt
      }
    }

    def semanticContent(implicit returnKeywordLiterals: Boolean = false): Option[TermExpr] = {
      if (cachedSemanticContent.contains(expr -> returnKeywordLiterals)) {
        cachedSemanticContent(expr -> returnKeywordLiterals)
      } else {
        val content = expr match {
          case Rule("SP", _, _, _) => None
          case c@CharIn(_, meaningful) => if (meaningful) Some(c) else None
          case Fragment(_, named, _) if named.headOption.contains("EOI") => None
          case Rule("whitespace", _, _, _) => None
          case Rule(name, lexer, inline, definition) =>
            if (lexer) {
              definition.semanticContent.map(_ => RuleRef(name))
            }
            else {
              Some(RuleRef(name))
            }
          case RuleRef(refName) => rules(refName).semanticContent
          case _: StringLiteral => None
          case cni: CharNotIn => Some(cni)
          case f: Fragment => Some(f)
          case i@IgnoreCaseLiteral(l, meaningful) =>
            if (meaningful) Some(i) else None
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
            val contentWithoutLiterals = e.semanticContent
            contentWithoutLiterals match {
              case None => e.semanticContent.map(Maybe(_)) //(returnKeywordLiterals = true)
              case Some(c) => Some(Maybe(c))
            }
          case Either(elements) =>
            val filtered = elements.flatMap(_.semanticContent)
            filtered match {
              case Seq() => None
              case Seq(one) => Some(one)
              case many => Some(Either(many))
            }
          case Sequence(elements) =>
            val filtered = elements.flatMap(_.semanticContent) //(returnKeywordLiterals = true)
            filtered match {
              case Seq() => None
              case Seq(first, r@RepSep(repeated, _, 0, _)) if first == repeated =>
                Some(r.copy(min = 1))
              case Seq(first, r@Rep(repeated, 0, _)) if first == repeated =>
                Some(r.copy(min = 1))
              case Seq(one) =>
                Some(one)
              case many => Some(Sequence(many))
            }
          case other => throw new UnsupportedOperationException(s"semanticContent cannot handle $other")
        }
        cachedSemanticContent += (expr, returnKeywordLiterals) -> content
        content
      }
    }

    def returnType: ScalaType = {
      if (cachedReturnTypes.contains(expr)) {
        cachedReturnTypes(expr)
      } else {
        val rt = expr match {
          case r@Rule(name, inline, lexer, definition) =>
            if (name == "SP" || name == "whitespace") {
              UnitType
            } else {
              val paramName = name.asParamName
              if (lexer && inline) {
                StringType(Some(paramName))
                //              if (r.parentTraits.isEmpty) {
                //                StringType(Some(paramName))
                //              } else {
                //                CaseClassType(name, List(StringType(Some("value"))), Some(paramName))
                //              }
              } else if (lexer) {
                StringType(Some(paramName))
                //              if (r.parentTraits.isEmpty) {
                //                StringType(Some(paramName))
                //              } else {
                //                CaseClassType(name, List(StringType(Some("value"))), Some(paramName))
                //              }
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
                        //tp
                        CaseClassType(name, List(tp))
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
                    case c: CharIn => c.returnType
                    case other =>
                      println(s"TODO:")
                      expr.show()
                      StringType(Some(paramName))
                  }
                }
              }
            }
          case RuleRef(refName) => rules(refName).returnType
          case Either(exprs) => TraitType(s"AnonymousTrait") //${exprs.mkString("Or")
          case Sequence(Seq(IgnoreCaseLiteral(keyword, false), r: RuleRef)) =>
            val innerType = r.returnType
            innerType.withParameterName(s"${
              keyword.toLowerCase
            }${
              innerType.nameAsParameter.firstCharToUpperCase
            }")
          case Sequence(parameters) =>
            TupleType(parameters.map(_.returnType))
          case r: Repeat => ListType(r.expr.returnType)
          case Maybe(parameter) =>
            val inner = parameter.returnType
            inner match {
              case b: BooleanType => b
              case _ => OptionType(inner)
            }
          case _: Fragment =>
            StringType()
          case IgnoreCaseLiteral(l, meaningful) =>
            if (meaningful) {
              BooleanType(Some(l.toLowerCase))
            } else {
              UnitType
            }
          case CharIn(_, meaningful) =>
            if (meaningful) {
              StringType()
            } else {
              UnitType
            }
          case _ =>
            println(s"TODO: $expr")
            StringType()
        }
        cachedReturnTypes += expr -> rt
        rt
      }
    }
  }

  private var cachedParentTraits: Map[GrammarExpr, List[String]] = Map.empty
  private var cachedReturnTypes: Map[GrammarExpr, ScalaType] = Map.empty
  private var cachedSemanticContent: Map[(GrammarExpr, Boolean), Option[TermExpr]] = Map.empty

}
