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
//import org.opencypher.parser.AntlrGrammarAst.Rule
//import org.opencypher.parser.AntlrGrammarParser.grammar
//
//import scala.io.Source
//import org.opencypher.parser.AntlrGrammarRewriters._
//
//object CypherGrammarParser extends App {
//
//  val cypherGrammarString = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("Cypher.g4")).mkString
//  val cypherGrammar = grammar.parse(cypherGrammarString).get.value
//  val flattened = flatten.rewrite(cypherGrammar)
//  val withIgnoreCase = optimizeIgnoreCase.rewrite(flattened)
//  val withCollectedIgnoreCase = collectIgnoreCaseLiterals.rewrite(withIgnoreCase)
////  val withRenamedRules = renameRules { name =>
////    if (name.startsWith("oC_")) {
////      s"${name.drop(3)}"
////    } else {
////      name
////    }
////  }.rewrite(withCollectedIgnoreCase)
//
//  val withCharIn = optimizeCharIn.rewrite(withCollectedIgnoreCase)
//
//  println(withCharIn.pretty)
//
//  // = "CypherAst"
//
//  //println(withCharIn.pretty)
////  withCharIn.foreach {
////    case r: Rule =>
////      implicit val astClassNamePrefix = ""
////      r.scalaAstClass(rootNodeName = "CypherAst").map(println)
////    case _ =>
////  }
//
//}
