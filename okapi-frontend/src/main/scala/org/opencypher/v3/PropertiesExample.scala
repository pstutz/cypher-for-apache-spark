//package org.opencypher.v3
//
//import fastparse.all._
//
//trait Properties
//
//object Properties {
//
//  def parser: P[Properties] = P { ((MapLiteral.parser.!) | (Parameter.parser.!)).! }
//
//}
//
//trait Parameter extends Properties with Atom
//
//object Parameter {
//
//  def parser: P[Parameter] = P { ((IgnoreCase("$").!) ~ (((SymbolicName.parser.!) | (DecimalInteger.parser.!)).!)).! }
//
//}
//
//trait SymbolicName extends Parameter with SchemaName
//
//object SymbolicName {
//
//  def parser: P[SymbolicName] = P { ((UnescapedSymbolicName.parser.!) | (EscapedSymbolicName.parser.!) | (HexLetter.parser.!)
//    //| (IgnoreCase("COUNT").!) | (IgnoreCase("FILTER").!) | (IgnoreCase("EXTRACT").!) | (IgnoreCase("ANY").!) | (IgnoreCase("NONE").!) | (IgnoreCase("SINGLE").!)).! }
//
//}
//
//trait Atom
//// removed, complex
