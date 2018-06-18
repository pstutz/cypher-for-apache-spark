package org.opencypher.v3

import fastparse.all._

object Example extends App {
  println(ZeroDigit.parser.parse("1"))
  println(ZeroDigit.parser.parse("0"))
  println(ExponentDecimalReal.parser.parse("hello"))
  println(ExponentDecimalReal.parser.parse("310E-30"))
}

case class ZeroDigit(value: String)

object ZeroDigit {

  def parser: P[ZeroDigit] = P { IgnoreCase("0").!.map(p => ZeroDigit(p)) }

}

case class NonZeroOctDigit(value: String)

object NonZeroOctDigit {

  def parser: P[NonZeroOctDigit] = P { ((IgnoreCase("1").!) | (IgnoreCase("2").!) | (IgnoreCase("3").!) | (IgnoreCase("4").!) | (IgnoreCase("5").!) | (IgnoreCase("6").!) | (IgnoreCase("7").!)).!.map(p => NonZeroOctDigit(p)) }

}

case class NonZeroDigit(value: String)

object NonZeroDigit {

  def parser: P[NonZeroDigit] = P { ((NonZeroOctDigit.parser.!) | (IgnoreCase("8").!) | (IgnoreCase("9").!)).!.map(p => NonZeroDigit(p)) }

}

case class ExponentDecimalReal(value: String) //extends DoubleLiteral

object ExponentDecimalReal {

  def parser: P[ExponentDecimalReal] = P { (((((((ZeroDigit.parser.!) | (NonZeroDigit.parser.!)).!.!).rep(min = 1).!) | ((((((ZeroDigit.parser.!) | (NonZeroDigit.parser.!)).!.!).rep(min = 1).!) ~ (IgnoreCase(".").!) ~ ((((ZeroDigit.parser.!) | (NonZeroDigit.parser.!)).!.!).rep(min = 1).!)).!) | (((IgnoreCase(".").!) ~ ((((ZeroDigit.parser.!) | (NonZeroDigit.parser.!)).!.!).rep(min = 1).!)).!)).!) ~ (((IgnoreCase("e").!) | (IgnoreCase("E").!)).!) ~ ((IgnoreCase("-").!).?.!) ~ ((((ZeroDigit.parser.!) | (NonZeroDigit.parser.!)).!.!).rep(min = 1).!)).!.map(p => ExponentDecimalReal(p)) }

}
