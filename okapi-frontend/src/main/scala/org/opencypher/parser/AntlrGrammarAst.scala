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
//import org.opencypher.caps.trees.AbstractTreeNode
//
//object AntlrGrammarAst {
//
//  def lowerCaseIfCamelCase(className: String): String = {
//    if (className.forall(_.isUpper)) {
//      className
//    } else {
//      val c = className.toCharArray
//      c(0) = Character.toLowerCase(c(0))
//      new String(c)
//    }
//  }
//
//  abstract class GrammarExpr extends AbstractTreeNode[GrammarExpr]
//
//  //{
//  //    def nodeParamName: String
//  //
//  //    // paramName, paramType
//  //    def nodeSignature: Map[String, String] = {
//  //       val className = getClass.getSimpleName
//  //      if (className.head.isUpper) { // Lexer rule
//  //        "String"
//  //      } else {
//  //        this match {
//  //          case Optional(expr) => Some(s"Option[${expr.nodeType}]")
//  //          case Sequence(h :: Nil) =>
//  //            h.nodeType
//  //          case Sequence(exprs) =>
//  //
//  //            s"List[]"
//  //        }
//  //      }
//  //    }
//  //  }
//
//
//  case class Grammar(name: String, rules: List[Rule]) extends GrammarExpr
//
//  abstract class Rule extends GrammarExpr {
//    def name: String
//  }
//
//  case class GrammarRule(name: String, grammarExpr: GrammarExpr) extends Rule
//
//  case class LexerRule(name: String, lexerExpr: GrammarExpr) extends Rule
//
//  case class Fragment(name: String, ranges: List[Range]) extends Rule
//
//  case class Range(from: String, to: Option[String]) extends GrammarExpr
//
//  case class RuleRef(ruleName: String) extends GrammarExpr
//
//  //    {
//  //      override def noPrefixClassName: String = ruleName
//  //    }
//
//  case class CharInLiteral(chars: String) extends GrammarExpr
//
//  case class IgnoreCaseLiteral(s: String) extends GrammarExpr
//
//  //    {
//  //      override def isParam = false
//  //    }
//
//  case class Literal(s: String) extends GrammarExpr
//
//  //    {
//  //      override def isParam = false
//  //
//  //      override def className(implicit astClassNamePrefix: String): String = "String"
//  //    }
//
//  case class Repeat(expr: GrammarExpr, min: Int) extends GrammarExpr
//
//  //    {
//  //      override def paramName: String = expr.paramName + "s"
//  //
//  //      override def className(implicit astClassNamePrefix: String): String = {
//  //        s"List[${expr.className}]"
//  //      }
//  //    }
//
//  case class Optional(expr: GrammarExpr) extends GrammarExpr
//
//  //    {
//  //      override def isParam = !expr.isSymbol
//  //
//  //      override def className(implicit astClassNamePrefix: String): String = {
//  //        s"Option[${expr.className}]"
//  //      }
//  //    }
//
//  case class Either(exprs: List[GrammarExpr]) extends GrammarExpr
//
//  case class Sequence(exprs: List[GrammarExpr]) extends GrammarExpr
//
//  //    {
//  //      override def isParam = exprs.forall(!_.isSymbol)
//  //    }
//
//}
