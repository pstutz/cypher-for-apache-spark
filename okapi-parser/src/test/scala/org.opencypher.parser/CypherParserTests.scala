package org.opencypher.parser

import org.scalatest.{FunSpec, Matchers}

class CypherParserTests extends FunSpec with Matchers {

  it("query") {
    CypherParser.parse("MATCH (a:A)\nMATCH (a)-[:LIKES*-2]->(c)\nRETURN c.name")
  }

  it("individual parser") {
    CypherParser.doubleLiteral.parse("1.34E999").get.value.show()
  }

}
