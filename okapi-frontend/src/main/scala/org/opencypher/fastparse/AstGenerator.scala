package org.opencypher.fastparse

import org.opencypher.okapi.trees.AbstractTreeNode

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

  //    val q = rules("MapLiteral")
  //    println(q)
  //    q.show()
  //    q.semanticContent.foreach(_.show())
  //    q.definition.semanticContent.foreach(_.show())
  //    println(q.returnType)
  //    println(q.returnType.typeSignature)
  //    println(q.returnType.scalaClassDef)


  //  val updating = rules("UpdatingPart")
  //  updating.show()

  //  rules.values.foreach { r =>
  //    val t = r.scalaType
  //    //    if (t.parents.nonEmpty) {
  //    println("\n============================================================\n")
  //    println(s"${r.name} EXPR tree")
  //    r.show
  //    println()
  //    println(s"${r.name} CONTENT tree")
  //    r.semanticContent.foreach(_.show())
  //    println()
  //    println(s"${r.name} EXTENDS tree")
  //    t.show()
  //    println()
  //    println("PARENT")
  //    t.parents.foreach(_.expr.show())
  //    println("\n============================================================\n")
  //    //    }
  //  }

  //  val scalaTypes = rules.values.map(_.returnType.scalaClassDef)
  //  println(scalaTypes.mkString("\n"))

  //  val scalaTypes = rules.values.map(_.scalaType)
  //
  //  println(scalaTypes.flatMap(_.toClass).mkString("\n"))


  //  val yieldItems = rules("YieldItems")
  //  yieldItems.show()
  //  yieldItems.semanticContent.map(_.show())
  //  println(yieldItems.scalaType)
  //  println(yieldItems.scalaType.toCaseClass)

  //
  //  //  rules.values.foreach { r =>
  //  //    val currentType = r.scalaType
  //  //    val implementations = scalaTypes.count(st => st.expr.semanticContent.isEmpty && st.parents.exists(_.containsTree(currentType)))
  //  //    if (implementations > 0) {
  //  //      println(s"${r.name} has $implementations implementations")
  //  //    }
  //  //  }
  //
  //  val atom = rules("Atom")
  //  val atomType = atom.scalaType
  //  val atomImpls = scalaTypes.filter { st =>
  //    st.parents.exists(_.containsTree(atomType))
  //  }
  //
  //  //st.expr.semanticContent.isEmpty &&
  //  //
  //  atomImpls.foreach { i =>
  //    val r = i.expr.asInstanceOf[Rule]
  //    if (r.inline || r.lexer) {
  //      println("Low-level: " + i.expr)
  //    } else {
  //      println("REAL: " + i.expr)
  //      i.show()
  //    }
  //
  //  }
  //val implementations = scalaTypes.count(st => st.expr.semanticContent.isEmpty && st.parents.exists(_.containsTree(currentType)))
  //  if (implementations > 0) {
  //    println(s"${r.name} has $implementations implementations")
  //  }


  //    rules.values.foreach { r =>
  //      println(r.name)
  //      r.semanticContent.foreach(_.show())
  //    }

  //  rules.values.foreach(_.show())

  //  val root = rules("Cypher")

  //  val rootType = root.scalaType
  //  rootType.show()

  //  val rootContent = root.semanticContent
  //  rootContent.map(_.show())

  //  rules.values.foreach { r =>
  //    println(r.name)
  //    r.semanticContent.foreach(_.show())
  //  }

  //  val e = rules("OrExpression")
  //  println("=======")
  //  e.show()
  //  e.semanticContent.foreach(_.show())
  //  e.scalaType.show()

  //    val not = rules("NotExpression")
  //    println("=======")
  //    not.show()
  //    not.semanticContent.foreach(_.show())
  //    not.scalaType.show()

  //println(classDefsWithParentClasses.mkString("\n"))
}

//case class OldScalaType(name: String, inline: Boolean, expr: GrammarExpr, parents: List[OldScalaType]) extends AbstractTreeNode[OldScalaType] {
//
//  def parentNames: List[String] = parents.flatMap(_.toList).filterNot(_.inline).map(_.name).sorted.distinct
//
////  def toClass(implicit h: ScalaTypeHelper): Option[String] = {
////    import h._
////    if (inline) {
////      None
////    } else {
////      val classDef = expr.semanticContent.flatMap(_.asParam) match {
////        case None => s"""trait $name"""
////        case Some(params) =>
////          val classString = s"""case class $name($params)"""
////          val extendString = parentNames match {
////            case Nil => ""
////            case h :: Nil => s" extends $h"
////            case h :: t => s" extends $h with ${t.mkString(" with ")}"
////          }
////          s"""$classString$extendString"""
////      }
////      Some(classDef)
////    }
////  }
//
//}

class ScalaTypeHelper(rules: Map[String, Rule]) {

  import StringUtilities._

  val keywordTerms: Set[IgnoreCaseLiteral] = rules("ReservedWord").collect { case l: IgnoreCaseLiteral => l }.toSet

  val keywords: Set[String] = keywordTerms.map(_.s.toUpperCase)

  implicit class GrammarExprConverter(expr: GrammarExpr) {

    //    def scalaTypeName: String = {
    //      expr match {
    //        case Rule(name, _, inline, ruleDef) =>
    //          if (inline) {
    //            ruleDef.scalaTypeName
    //          } else {
    //            name
    //          }
    //        case RuleRef(refName) => rules(refName).scalaTypeName
    //        case other => other.scalaType.parentNames.headOption.getOrElse(s"String(\n${other.pretty})\n")
    //      }
    //    }

    //    def asParam: Option[String] = {
    //      expr match {
    //        case RuleRef(refName) => rules(refName).asParam
    //        case Rule(name, _, _, definition) =>
    //          val rule = rules(name)
    //          if (rule.lexer) {
    //            Some(s"${name.asParamName}: String")
    //          } else {
    //            if (rule.inline) {
    //              Some(s"${name.asParamName}: ${rule.semanticContent.map(_.scalaType.name).getOrElse(s"String($expr)")}")
    //            } else {
    //              Some(s"${name.asParamName}: $name")
    //            }
    //          }
    //        //          definition.semanticContent match {
    //        //            case Some(RuleRef(refName)) => rules(refName).asParam
    //        //            case Some(r: Repeat) =>
    //        //              val maybeContent = r.expr.semanticContent
    //        //              maybeContent match {
    //        //                case None => None
    //        //                case Some(c) => Some(s"${name.asParamName}: List[${c.scalaType.name}]")
    //        //              }
    //        //            case Some(Maybe(e)) =>
    //        //              val maybeContent = e.semanticContent
    //        //              maybeContent match {
    //        //                case None => None
    //        //                case Some(c) => Some(s"${name.asParamName}: Option[${e.scalaType.name}]")
    //        //              }
    //        //            case Some(Sequence(elements)) => Some(s"${elements.flatMap(_.asParam).mkString(", ")}")
    //        //            case None => None
    //        //            case other => None
    //        //          }
    //        case r: Repeat => r.expr.asParam.map(p => s"${r.expr.scalaType.name.firstCharToLowerCase}: List[${r.scalaType.name}]")
    //        case _ => None
    //      }
    //    }

    def semanticContent: Option[TermExpr] = {
      if (cachedSemanticContent.contains(expr)) {
        cachedSemanticContent(expr)
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
            if (keywords.contains(l.toUpperCase)) {
              Some(i)
            } else {
              None
            }
          case r: Rep =>
            val maybeContent = r.expr.semanticContent
            maybeContent match {
              case None =>
                r.expr.filter(_.containsTree())
                None
              case Some(c) => Some(r.copy(expr = c))
            }
          case r: RepSep =>
            val maybeContent = r.expr.semanticContent
            maybeContent match {
              case None => None
              case Some(c) => Some(r.copy(expr = c))
            }
          case Maybe(e) =>
            e.semanticContent.map(Maybe(_))
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
        cachedSemanticContent += expr -> content
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
            //          if (lexer | inline) {
            //            semanticContent.map(_.returnType.withParameterName(paramName))
            //              .getOrElse(StringType(Some(paramName)))
            //          } else {
            semanticContent match {
              case None => StringType(Some(paramName))
              case Some(content) => content match {
                case RuleRef(refName) =>
                  CaseClassType(name, List(rules(refName).returnType), Some(paramName))
                case Either(_) =>
                  //                if (lexer) {
                  //                  StringType(Some(paramName))
                  //                } else {
                  TraitType(name)
                //                }
                //                val contents = alternatives.flatMap(_.semanticContent.toSet) //.map(_.returnType)
                //                if (contents.forall {
                //                  case RuleRef(refName) =>
                //                    val rule = rules(refName)
                //                    if (rule.lexer) {
                //                      true
                //                    } else {
                //                      false
                //                    }
                //                  case _ => true
                //                }) {
                //                  StringType(Some(paramName))
                //                } else {
                //                  TraitType(name)
                //                  if (contents.forall {
                //                    case _: RuleRef => true
                //                    case _ => true
                //                  }) {
                //                    TraitType(name)
                //                  } else {
                case Sequence(parameters) =>
                  //                if (inline) {
                  //                  TupleType(parameters.map(_.returnType), Some(paramName))
                  //                } else {
                  CaseClassType(name, parameters.map(_.returnType))
                //                }
                case r: Repeat =>
                  val tp = r.returnType //.withParameterName(paramName)
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
          //          parameter match {
          //            case l: IgnoreCaseLiteral => l.returnType
          //            case _ =>
          //          }
          OptionType(parameter.returnType)
        //          if (parameter.semanticContent.isEmpty && keywordTerms.exists(parameter.containsTree)) {
        //            val name = parameter.collect { case IgnoreCaseLiteral(l) if keywords.contains(l) => l }.mkString("-")
        //            BooleanType(Some(name))
        //          } else {
        //
        //          }
        case _: Fragment =>
          StringType()
        case IgnoreCaseLiteral(l) =>
          BooleanType(Some(l.toLowerCase))
        case _ =>
          println(s"TODO: $expr")
          StringType()
      }
    }

    //    def scalaType: OldScalaType = {
    //      if (cachedTypes.contains(expr)) {
    //        cachedTypes(expr)
    //      } else {
    //        val initialType = expr match {
    //          case Rule(name, inline, _, definition) => OldScalaType(name, inline, definition, List.empty)
    //          case RuleRef(refName) => rules(refName).scalaType
    //          case r: Repeat => OldScalaType(s"${r.expr.scalaType.name}", true, r, List.empty)
    //          case other =>
    //            val name = other.semanticContent.map(_.scalaType).map(_.name).getOrElse("String")
    //            OldScalaType(s"String($other)", true, other, List.empty)
    //          //            if (visited.contains(other.sca))
    //          //            val name = other.semanticContent.map(_.scalaType).map(_.name).getOrElse("String")
    //        }
    //        val computedScalaType = rules.values.filterNot(_ == expr).foldLeft(initialType) {
    //          case (currentType, parent@Rule(_, _, _, ruleDef)) =>
    //            ruleDef.semanticContent match {
    //              case Some(Either(childExprs)) =>
    //                if (childExprs.contains(expr)) {
    //                  currentType.copy(parents = parent.scalaType :: currentType.parents)
    //                } else {
    //                  expr match {
    //                    case Rule(n, _, _, _) =>
    //                      if (childExprs.contains(RuleRef(n))) {
    //                        if (!currentType.parents.map(_.name).contains(parent.name)) {
    //                          currentType.copy(parents = parent.scalaType :: currentType.parents)
    //                        } else {
    //                          currentType
    //                        }
    //                      } else {
    //                        currentType
    //                      }
    //                    case _ => currentType
    //                  }
    //                }
    //              case Some(e) if expr == e =>
    //                currentType.copy(parents = parent.scalaType :: currentType.parents)
    //              case Some(RuleRef(name)) =>
    //                expr match {
    //                  case Rule(n, _, _, _) if name == n =>
    //                    currentType.copy(parents = parent.scalaType :: currentType.parents)
    //                  case _ => currentType
    //                }
    //              case _ => currentType
    //            }
    //        }
    //        cachedTypes += expr -> computedScalaType
    //        computedScalaType
    //      }
    //    }

  }

  //  private var cachedTypes: Map[GrammarExpr, OldScalaType] = Map.empty

  private var cachedSemanticContent: Map[GrammarExpr, Option[TermExpr]] = Map.empty

}


//val classDefs: Map[String, ScalaClassDefinition] = rules.values.collect {
//case r@Rule(name, _, _, _) => name -> ScalaClassDefinition(name, r)
//}.toMap
//case class ScalaClassDefinition(name: String, expr: GrammarExpr, inheritance: List[String] = List.empty)
