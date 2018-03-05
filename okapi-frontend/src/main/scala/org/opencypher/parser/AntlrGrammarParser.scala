///*
// * Copyright (c) 2016-2018 "Neo4j, Inc." [https://neo4j.com]
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.opencypher.parser
//
//import fastparse.all._
//import org.opencypher.parser.AntlrGrammarAst._
//
//object AntlrGrammarParser {
//
//  // AST Tree nodes
//  val grammar = P(header ~/ rules).map(t => Grammar(t._1, t._2))
//  val rules = P(comment.? ~ rule ~/ (SP ~ comment.? ~ SP ~ rule).rep).map { case (r, rs) => r :: rs.toList }
//  val rule = P(grammarRule | lexerRule | fragment)
//  val grammarRule = P(nonCapitalIdentifier ~/ SP ~/ ":" ~/ SP ~ ruleRhs).map(t => GrammarRule(t._1, t._2))
//  val lexerRule = P(identifier ~/ SP ~/ ":" ~/ SP ~ ruleRhs).map(t => LexerRule(t._1, t._2))
//  val ruleRhs = P(sequence ~ SP ~/ ";" ~/ SP)
//
//  val fragment: Parser[Fragment] = P("fragment" ~ SP ~ identifier ~ SP ~ ":" ~ SP ~ "[" ~ range.rep ~ "]").
//    map(t => Fragment(t._1, t._2.toList))
//  val range: Parser[Range] = P(character ~ ("-" ~ character).?).map(t => Range(t._1, t._2))
//  val character: Parser[String] = P((!CharIn("\\") | escape).!)
//  val escapedFragmentChar = P("\\" ~ CharIn("]\\"))
//  val unescapedFragmentChars = P(CharsWhile(!"\\]".contains(_)))
//  val unicodeEscape = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)
//  val escape = P("\\" ~ (CharIn("\"/\\bfnrt") | unicodeEscape))
//  val hexDigit = P(CharIn('0' to '9', 'a' to 'f', 'A' to 'F'))
//
//  // Rule RHS expressions
//  val sequence: Parser[GrammarExpr] = P(either ~ (SP ~ sequence).rep).map {
//    case (expr, exprs) => exprs match {
//      case Nil => expr
//      case seq => Sequence(expr :: seq.toList)
//    }
//  }
//  val either = P(repeat ~ (SP ~ "|" ~ SP ~ sequence).rep).map {
//    case (expr, exprs) => exprs match {
//      case Nil => expr
//      case seq => Either(expr :: seq.toList)
//    }
//  }
//  val repeat = P(factor ~ SP ~ CharIn("+*?").?.!).map {
//    case (a, m) => m match {
//      case "+" => Repeat(a, 1)
//      case "*" => Repeat(a, 0)
//      case "?" => Optional(a)
//      case _ => a
//    }
//  }
//  val factor = P(parentheses | literal | ruleRef)
//  val literal = P("'" ~/ (unescapedLiteralChars | escapedLiteralChars).rep.! ~ "'").map(Literal)
//  val parentheses = P("(" ~/ SP ~ sequence ~ SP ~ ")")
//  val ruleRef = P(identifier.!).map(RuleRef)
//
//  // Basic parsers
//  val newline = P("\n" | "\r\n" | "\r" | "\f")
//  val whitespace = P(" " | "\t" | newline)
//  val SP: Parser[Unit] = P(whitespace.rep)
//  val comment = P("/*" ~/ (!"*/" ~ AnyChar).rep ~/ "*/")
//  val header: Parser[String] = P(comment.? ~/ SP ~/ "grammar" ~/ SP ~/ CharsWhile(_ != ';').! ~/ ";" ~/ SP)
//  val nonCapitalIdentifier: Parser[String] = P(CharIn('a' to 'z', 'A' to 'Z').! ~ identifier.?).map { case (f, tail) =>
//    s"$f${tail.getOrElse("")}"
//  }
//  val identifier: Parser[String] = P(CharIn('a' to 'z', 'A' to 'Z', '0' to '9', "_").rep(min = 1).!)
//  val escapedLiteralChars = P("\\" ~ CharIn("\'\\"))
//  val unescapedLiteralChars = P(CharsWhile(!"\\'".contains(_)))
//
//}
