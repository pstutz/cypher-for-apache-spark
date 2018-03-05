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
//import org.opencypher.caps.trees.{BottomUp, TopDown}
//import org.opencypher.parser.AntlrGrammarAst._
//
//object AntlrGrammarRewriters {
//
//  val flatten: BottomUp[GrammarExpr] = {
//    BottomUp[GrammarExpr] {
//      case Sequence(exprs) =>
//        val flattened = exprs.flatMap {
//          case Sequence(exprs) => exprs
//          case other => List(other)
//        }
//        Sequence(flattened)
//      case Either(exprs) =>
//        val flattened = exprs.flatMap {
//          case Either(exprs) => exprs
//          case other => List(other)
//        }
//        Either(flattened)
//    }
//  }
//
//  val optimizeIgnoreCase: BottomUp[GrammarExpr] = BottomUp {
//    case Either(List(Literal(a), Literal(b))) if a.equalsIgnoreCase(b) => IgnoreCaseLiteral(a)
//  }
//
//  val collectIgnoreCaseLiterals: BottomUp[GrammarExpr] = BottomUp {
//    case Sequence(exprs) if exprs.forall(_.isInstanceOf[IgnoreCaseLiteral]) =>
//      IgnoreCaseLiteral(exprs.map(_.asInstanceOf[IgnoreCaseLiteral].s).mkString)
//  }
//
////  def renameRules(rename: String => String): BottomUp[GrammarExpr] = BottomUp {
////    case Rule(name, expr) => Rule(rename(name), expr)
////    case RuleRef(name) => RuleRef(rename(name))
////  }
//
//  val optimizeCharIn: BottomUp[GrammarExpr] = BottomUp {
//    case e@Either(exprs) if exprs.forall(_.isInstanceOf[Literal]) =>
//      val literals = exprs.map(_.asInstanceOf[Literal].s)
//      if (literals.forall(_.length == 1)) {
//          CharInLiteral(literals.mkString)
//      } else {
//        e
//      }
//  }
//
//
//}
