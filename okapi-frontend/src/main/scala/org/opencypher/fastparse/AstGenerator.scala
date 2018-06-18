package org.opencypher.fastparse

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
    //println(r.returnType)
    println()
    println(r.returnType.typeSignature)
    r.returnType.scalaClassDef(r.parentTraits).foreach(println)
    println("\n============================================================\n")
  }

  rules.values.foreach { r =>
    r.returnType.scalaClassDef(r.parentTraits).foreach { d =>
      if (!d.contains("Anonymous") && !d.contains("Boolean")) {
        println(d)
      }
    }
  }

}

class ScalaTypeHelper(rules: Map[String, Rule]) {

  import StringUtilities._

  val keywordTerms: Set[IgnoreCaseLiteral] = rules("ReservedWord").collect { case l: IgnoreCaseLiteral => l }.toSet

  val keywords: Set[String] = keywordTerms.map(_.s.toUpperCase)

  implicit class GrammarExprConverter(expr: GrammarExpr) {

    def parserString(implicit ruleMap: Map[String, Rule]): String = {
      expr match {
        case Rule(_, _, _, definition) => definition.parserString
        case RuleRef(refName) => s"$refName.parse"
        case StringLiteral(s) => s"$s"
        case Fragment(ps, _, _) => // TODO: Handle named sets
          val unicodeChars = ps.map(codePoint => Character.toString(codePoint.toChar)).mkString
          s"""CharIn("$unicodeChars").!"""
        case IgnoreCaseLiteral(s: String) =>
        s"""IgnoreCase("$s").!"""
        case RepSep(expr: TermExpr, sep: TermExpr, min: Int, maxOpt: Option[Int], nameOpt: Option[String]) extends Repeat {
          override def parserString(implicit ruleMap: Map[String, Rule]): String = {
            val sepString = Some(s"sep = (${sep.parserString})")
            val repArgs = List(sepString, minString, maxString).flatten.mkString(", ")
            s"(${expr.parserString}).rep($repArgs)"
          }
        }

        //case class SimpleRepeat(expr: GrammarExpr, min: Int, maxOpt: Option[Int], nameOpt: Option[String] = None) extends Repeat {
        //  override def parserString(implicit ruleMap: Map[String, Rule]): String = {
        //    val repArgs =  List(minString, maxString).flatten.mkString(", ")
        //    if (repArgs == "") {
        //      s"(${expr.parserString}).rep"
        //    } else {
        //      s"(${expr.parserString}).rep($repArgs)"
        //    }
        //  }
        //}
        //
        //case class Optional(expr: GrammarExpr, nameOpt: Option[String] = None) extends TermExpr {
        //  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
        //    expr.scalaType match {
        //      case l@Some(ListType(_)) => l
        //      case o@Some(OptionType(_)) => o
        //      case other => other.map(OptionType)
        //    }
        //  }
        //
        //  override def parserString(implicit ruleMap: Map[String, Rule]): String = s"(${expr.parserString}).?"
        //}
        //
        //case class Either(exprs: List[GrammarExpr], nameOpt: Option[String] = None) extends TermExpr {
        //  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
        //    val className = nameOpt.getOrElse {
        //      s"${
        //        nameOpt.getOrElse {
        //          s"GeneratedAbstractClass${exprs.hashId}"
        //        }
        //      }"
        //    }
        //    Some(AbstractClassType(className))
        //  }
        //
        //  override def parserString(implicit ruleMap: Map[String, Rule]): String = {
        //    s"(${exprs.map(e => s"(${e.parserString})").mkString(" | ")})"
        //  }
        //}
        //
        //case class Sequence(exprs: List[TermExpr], nameOpt: Option[String] = None) extends TermExpr {
        //  override def scalaType(implicit ruleMap: Map[String, Rule]): Option[ScalaType] = {
        //    val elementTypes = exprs.flatMap(_.scalaType)
        //    if (elementTypes.size == 1) {
        //      Some(elementTypes.head)
        //    } else if (elementTypes.isEmpty) {
        //      None
        //    } else {
        //      val name = generateRuleName(exprs)
        //      val parametersWithTypes = exprs.flatMap(e => e.scalaType.map(e -> _)).toMap
        //      val params = parametersWithTypes.map { case (expr, typ) => Parameter(expr.parameterName, typ) }.toList
        //      val c = CaseClassType(name.asParamName, params, None)
        //      Some(c)
        //      //throw new Exception(s"Could not determine element type for list: elements ${exprs} with types $elementTypes")
        //    }
        //  }
        //
        //  override def parserString(implicit ruleMap: Map[String, Rule]): String = {
        //    s"(${exprs.map(e => s"(${e.parserString})").mkString(" ~ ")})"
        //  }
        //}
        case _ => ""
      }
    }

    def parentTraits: List[String] = {
      expr match {
        case Rule(name, _, _, _) => RuleRef(name).parentTraits
        case _ =>
          rules.values.flatMap { rule =>
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
          }.toList
      }
    }

    def semanticContent(implicit returnKeywordLiterals: Boolean = false): Option[TermExpr] = {
      if (cachedSemanticContent.contains(expr -> returnKeywordLiterals)) {
        cachedSemanticContent(expr -> returnKeywordLiterals)
      } else {
        val content = expr match {
          case Rule("SP", _, _, _) => None
          case Fragment(_, named, _) if named.headOption.contains("EOI") => None
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
            if (returnKeywordLiterals && keywords.contains(l.toUpperCase)) {
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
            val contentWithoutLiterals = e.semanticContent
            contentWithoutLiterals match {
              case None => e.semanticContent(returnKeywordLiterals = true).map(Maybe(_))
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
            val filtered = elements.flatMap(_.semanticContent(returnKeywordLiterals = true))
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
      expr match {
        case Rule(name, inline, lexer, definition) =>
          val paramName = name.asParamName
          if (lexer && inline) {
            StringType(Some(paramName))
          } else if (lexer) {
            CaseClassType(name, List(StringType(Some("value"))), Some(paramName))
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
                case other =>
                  println(s"TODO:")
                  expr.show()
                  StringType(Some(paramName))
              }
            }
          }
        //          }
        case RuleRef(refName) => rules(refName).returnType
        case Either(exprs) => TraitType(s"AnonymousTrait") //${exprs.mkString("Or")
        case Sequence(Seq(IgnoreCaseLiteral(keyword), r: RuleRef)) =>
          val innerType = r.returnType

          innerType.withParameterName(s"${keyword.toLowerCase}${innerType.nameAsParameter.firstCharToUpperCase}")
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
