package org.opencypher.parser

import org.scalatest.{FunSpec, Matchers}

class CypherParserTests extends FunSpec with Matchers {

  it("can parse a match clause") {
    CypherParser.matchClause.parse("MATCH (n)").get.value.show()
  }

}
