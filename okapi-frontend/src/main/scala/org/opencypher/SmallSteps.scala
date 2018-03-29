package org.opencypher

import java.util.concurrent.atomic.AtomicInteger

import org.opencypher.okapi.trees.BottomUp
import org.opencypher.tools.grammar.Helpers._

object SmallSteps extends App {
  val anonCounter = new AtomicInteger()

  val rules: Map[String, Rule] = CypherGrammar.parserRules()

  //TODO: Cover all cases elegantly: Arbitrary many elements before first, and arbitrarily many elements of type None as separators
  // Repeat with separator
  def repSep(implicit ruleMap: Map[String, Rule]) = BottomUp[GrammarExpr] {
    case Sequence(List(a, SimpleRepeat(Sequence(inner, innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
      if a == inner.last && inner.dropRight(1).forall(_.scalaType.isEmpty) =>
      val nameOpt: Option[String] = List(seqNameOpt, repNameOpt, innerSeqName, a.scalaType.map(t => (t.toString + "s").asParamName)).flatten.headOption
      RepeatWithSeparator(a, Sequence(inner.dropRight(1)), min + 1, maxOpt.map(_ + 1), nameOpt)

    // TODO: Create rewriting facilities to express this concisely
    case Sequence(List(p1, a, SimpleRepeat(Sequence(inner, innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
      if a == inner.last && inner.dropRight(1).forall(_.scalaType.isEmpty) =>
      val nameOpt: Option[String] = List(seqNameOpt, repNameOpt, innerSeqName, a.scalaType.map(t => (t.toString + "s").asParamName)).flatten.headOption
      Sequence(List(p1, RepeatWithSeparator(a, Sequence(inner.dropRight(1)), min + 1, maxOpt.map(_ + 1), nameOpt)), seqNameOpt)

    // TODO: Create rewriting facilities to express this concisely
    case Sequence(List(p1, p2, a, SimpleRepeat(Sequence(inner, innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
      if a == inner.last && inner.dropRight(1).forall(_.scalaType.isEmpty) =>
      val nameOpt: Option[String] = List(seqNameOpt, repNameOpt, innerSeqName, a.scalaType.map(t => (t.toString + "s").asParamName)).flatten.headOption
      Sequence(List(p1, p2, RepeatWithSeparator(a, Sequence(inner.dropRight(1)), min + 1, maxOpt.map(_ + 1), nameOpt)), seqNameOpt)

    // TODO: Create rewriting facilities to express this concisely
    case Sequence(List(p1, p2, p3, a, SimpleRepeat(Sequence(inner, innerSeqName), min, maxOpt, repNameOpt)), seqNameOpt)
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

  val rootExpressionType = AbstractClassType("CypherAst", Some(AbstractClassType("AbstractTreeNode[CypherAst]")))

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
            case List() => Some(rootType) // EITHER would fail with parent rule name, example: RightArrowHead : Some(AbstractClassType(parentRuleName))
            case List(_) =>
              val pt = parentType(parentRuleName, rootType)
              if (!pt.contains(rootExpressionType)) {
                pt
              } else {
                Some(AbstractClassType(parentRuleName))
              }
            case _ => Some(rootType)
          }
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

  implicit class ParserGenerator(val e: GrammarExpr) extends AnyVal {
    def asParser(implicit ruleMap: Map[String, Rule]): String = {
      val resultType = e.scalaType.getOrElse(StringType)
      resultType match {
        case StringType =>
        s"  def apply(s: String): P[String] = P ( ${e.parserString} )"
        case r =>
        s"  def apply(s: String): P[$r] = P ( ${e.parserString} ).map(${r.toString})"
      }
    }
  }

  implicit class ClassGenerator(val r: Rule) extends AnyVal {
    def asScalaClass(implicit ruleMap: Map[String, Rule], rootClass: AbstractClassType = rootExpressionType): Option[String] = {
      r.scalaType match {
        case Some(CaseClassType(name, parameters, superClassOption)) =>
          Some(
            s"""|case class $name(${parameters.mkString(", ")}) extends ${superClassOption.getOrElse(rootClass)}
                |object $name {
                |  ${r.asParser}
                |}
                |/**
                |Grammar expression:
                |${r.pretty}*/""".stripMargin)
        case Some(AbstractClassType(name, superClassOption)) =>
          val implementations = r.definition match {
            case e: Either => e.exprs.flatMap {
              _ match {
                case RuleRef(_) => None
                case l: IgnoreCaseLiteral => Some(s"case object ${l.s} extends ${name}")
                case s: Sequence =>
                  if (s.nonEmpty && s.exprs.head.isInstanceOf[Literal]) {
                    val cn = s.exprs.flatMap {
                      case i: IgnoreCaseLiteral => Some(i.s)
                      case l: Literal => l.nameOpt
                      case _ => None
                    }.mkString("").letters
                    //                    val alphaName = nameWithEverything.flatMap(_.)
                    if (s.scalaType.isEmpty) {
                      Some(s"case object $cn extends $name")
                    } else {
                      None // TODO: Implement
                    }
                  } else {
                    // TODO: Generate case classe and put common parameter accessors into the abstract class
                    s.scalaType.map(_ => s"case class ${name}Impl${s.hashId}(${/** TODO: Parameters **/}) extends $name")
                  }
                case _ => List.empty // TODO: Cover case
              }
            }
            case _ => List.empty // TODO: Cover case
          }
          val abstractParentClass =
            s"""|abstract class $name extends ${superClassOption.getOrElse(rootClass)}
                |/**
                |Grammar expression:
                |${r.pretty}*/""".stripMargin
          val allClasses = abstractParentClass :: implementations
          Some(allClasses.mkString("\n"))
        case _ =>
          None
        //throw new Exception(s"Could not turn $r into a scala class")
      }
    }

  }


}


