package org.opencypher.parser

import org.scalatest.{FunSpec, Matchers}

class CypherParserTests extends FunSpec with Matchers {

  it("foo") {
    CypherParser.parse("MATCH (a:A)\nMATCH (a)-[:LIKES*2..]->(c)\nRETURN c.name")
  }

}
