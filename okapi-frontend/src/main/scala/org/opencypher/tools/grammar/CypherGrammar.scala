package org.opencypher.tools.grammar

import java.io.File

import org.opencypher.AbstractClassType
import org.opencypher.grammar._
import org.opencypher.okapi.trees.{AbstractTreeNode, BottomUp}
import org.opencypher.tools.grammar.Helpers._


abstract class GrammarExpr extends AbstractTreeNode[GrammarExpr] {
  def nameOpt: Option[String]
}

case class Rule(name: String, parentClassOpt: Option[AbstractClassType], inline: Boolean, lexer: Boolean, definition: GrammarExpr) extends GrammarExpr {
  override def nameOpt: Option[String] = Some(name)
}

abstract class TermExpr extends GrammarExpr

case class RuleRef(ruleName: String) extends TermExpr {
  override def nameOpt: Option[String] = Some(ruleName)
}

abstract class Literal extends TermExpr

case class StringLiteral(s: String) extends Literal {
  override def nameOpt: Option[String] = Some(s)
}

case class CharNotIn(nameOpt: Option[String], chars: String) extends Literal

case class CharIn(nameOpt: Option[String], chars: String) extends Literal

case class Fragment(nameOpt: Option[String], ps: Set[Int], namedInclusions: Set[String], namedExclusions: Set[String]) extends Literal

case class IgnoreCaseLiteral(nameOpt: Option[String], s: String) extends Literal

case class RepeatWithSeparator(expr: TermExpr, sep: TermExpr, min: Int, maxOpt: Option[Int], nameOpt: Option[String]) extends TermExpr

case class Repeat(expr: GrammarExpr, min: Int, maxOpt: Option[Int], nameOpt: Option[String] = None) extends TermExpr

case class Optional(expr: GrammarExpr, nameOpt: Option[String] = None) extends TermExpr

case class Either(exprs: List[GrammarExpr], nameOpt: Option[String] = None) extends TermExpr

case class Sequence(exprs: List[TermExpr], nameOpt: Option[String] = None) extends TermExpr

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

//  val inlineEitherWithoutChoice: BottomUp[GrammarExpr] = BottomUp {
//    case r@Rule(_, _, _, Either(List(single))) =>
//      r.copy(definition = single)
//  }
//
//  val withInlinedSimpleEither = parsedTrees.mapValues(inlineEitherWithoutChoice.rewrite(_).asInstanceOf[Rule])

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
//
//
//  implicit val trees: Map[String, Rule] = withInlinedSimpleEither
//
//  val rootRule = trees("Cypher")
//
//  //  recurse(rootRule, 9)
//
//  val d = trees("RegularQuery")
//  recurse(d)
//
//  def recurse(r: Rule, depth: Int = 1, visited: Set[String] = Set.empty, printStops: Boolean = false): Unit = {
//    if (depth > 0) {
//      debug(r)
//      if (depth > 1) {
//        val subRules = r.definition.collect { case RuleRef(name) => name }.toSet -- visited
//        subRules.foreach { sr =>
//          if (sr.asScala.isDefined || sr.definition.isInstanceOf[Either]) {
//            recurse(trees(sr.name), depth - 1, visited + sr.name)
//          } else if (printStops) {
//            println(s"Stopped at ${sr.name}")
//          }
//        }
//      }
//    }
//  }
//
//  def debug(t: Rule): Unit = {
//    println("\n=======================================================================================")
//    println(t.pretty)
//
//    //t.asScala.map(_.pretty).map(println)
//    val astClass = t.generateAstClass
//    astClass match {
//      case Some(s) =>
//        println(s)
//      case None =>
//    }
//    println("=======================================================================================\n")
//  }

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
//        t.GrammarAsScala.map(println)
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


