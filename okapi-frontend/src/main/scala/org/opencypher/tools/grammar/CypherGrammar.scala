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
}
