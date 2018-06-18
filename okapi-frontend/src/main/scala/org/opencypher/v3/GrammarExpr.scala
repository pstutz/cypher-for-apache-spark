package org.opencypher.v3

import org.opencypher.okapi.trees.AbstractTreeNode

import scala.collection.immutable.List

sealed abstract class GrammarExpr extends AbstractTreeNode[GrammarExpr]

case class Rule(name: String, inline: Boolean, lexer: Boolean, definition: GrammarExpr) extends GrammarExpr

abstract class TermExpr extends GrammarExpr

case class RuleRef(ruleName: String) extends TermExpr

abstract class Literal extends TermExpr

case class StringLiteral(s: String) extends Literal

case class CharNotIn(chars: String) extends Literal

case class CharIn(chars: String) extends Literal

case class Fragment(ps: Set[Int], namedInclusions: Set[String], namedExclusions: Set[String]) extends Literal

case class IgnoreCaseLiteral(s: String) extends Literal

abstract class Repeat extends TermExpr {
  def expr: GrammarExpr

  def min: Int

  def maybeMax: scala.Option[Int]
}

case class Rep(expr: GrammarExpr, min: Int, maybeMax: scala.Option[Int]) extends Repeat

case class RepSep(expr: TermExpr, sep: TermExpr, min: Int, maybeMax: scala.Option[Int]) extends Repeat

case class Maybe(expr: GrammarExpr) extends TermExpr

case class Either(exprs: List[GrammarExpr]) extends TermExpr

case class Sequence(exprs: List[TermExpr]) extends TermExpr
