package org.opencypher.parser

import org.scalatest.{FunSpec, Matchers}

class CypherParserTests extends FunSpec with Matchers {

  it("query") {
    CypherParser.parse("MATCH (a:A), (b:B)\nWITH [p = (a)-[*]->(b) | p] AS paths, count(a) AS c\nRETURN paths, c")
  }

  it("individual parser") {
    CypherParser.patternComprehension.parse("[p = (a)-[*]->(b) | p]").get.value.show()
  }

}
