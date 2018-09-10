package org.opencypher.parser

import org.scalatest.{FunSpec, Matchers}

class CypherParserTests extends FunSpec with Matchers {

  it("foo") {
    CypherParser.parse("RETURN 1e-9")
  }

}
