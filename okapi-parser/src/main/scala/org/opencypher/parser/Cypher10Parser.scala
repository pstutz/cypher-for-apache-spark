/*
 * Copyright (c) 2016-2018 "Neo4j Sweden, AB" [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Attribution Notice under the terms of the Apache License 2.0
 *
 * This work was created by the collective efforts of the openCypher community.
 * Without limiting the terms of Section 6, any Derivative Work that is not
 * approved by the public consensus process of the openCypher Implementers Group
 * should not be described as “Cypher” (and Cypher® is a registered trademark of
 * Neo4j Inc.) or as "openCypher". Extensions by implementers or prototypes or
 * proposals for change that have been documented or implemented should only be
 * described as "implementation extensions to Cypher" or as "proposed changes to
 * Cypher that are not yet approved by the openCypher community".
 */
package org.opencypher.parser

import org.antlr.v4.runtime._
import org.opencypher.okapi.api.exception.CypherException
import org.opencypher.okapi.api.exception.CypherException.ErrorPhase.CompileTime
import org.opencypher.okapi.api.exception.CypherException.ErrorType.SyntaxError
import org.opencypher.okapi.api.exception.CypherException.{ErrorDetails, ErrorPhase, ErrorType, InvalidUnicodeLiteral}
import org.opencypher.okapi.api.value.CypherValue._

import scala.collection.JavaConverters._

object Cypher10Parser {

  def parse(query: String, parameters: Map[String, CypherValue] = Map.empty): CypherAst = {
    val input = CharStreams.fromString(query)
    val lexer = new CypherLexer(input)
    lexer.removeErrorListeners()
    lexer.addErrorListener(LexerErrorListerner(lexer, query))
    val tokens = new CommonTokenStream(lexer)
    val parser = new CypherParser(tokens)
    val tree = parser.oC_Cypher
    val ast = AntlrAstTransformer.visit(tree)
    ast
  }

}

case class LexerException(override val errorType: ErrorType, override val phase: ErrorPhase, override val detail: ErrorDetails)
  extends CypherException(errorType, phase, detail)

case class LexerErrorListerner(lexer: CypherLexer, string: String) extends BaseErrorListener {
  override def syntaxError(
    recognizer: Recognizer[_, _],
    offendingSymbol: scala.Any,
    lineNumber: Int,
    charPositionInLine: Int,
    msg: String,
    e: RecognitionException
  ): Unit = {
    val line = string.split("\n")
    val offendingLine = line(lineNumber - 1)
    val msg =
      s"""|on line $lineNumber, character $charPositionInLine:
          |\t$offendingLine
          |\t${"~" * charPositionInLine}^${"~" * (offendingLine.length - charPositionInLine)}""".stripMargin
    throw LexerException(SyntaxError, CompileTime, InvalidUnicodeLiteral(msg))
  }


//  SqlBaseParser.QueryContext queryContext = context.query();
//  int a = queryContext.start.getStartIndex();
//  int b = queryContext.stop.getStopIndex();
//  Interval interval = new Interval(a,b);
//  String viewSql = context.start.getInputStream().getText(interval);

}
