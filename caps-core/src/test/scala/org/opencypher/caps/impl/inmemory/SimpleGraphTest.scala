package org.opencypher.caps.impl.inmemory

import org.scalatest.{FunSuite, Matchers}

class SimpleGraphTest extends FunSuite with Matchers {

  test("match a trivial query") {
    implicit val session = SimpleSession()

    // Given
    val given = SimpleGraph("""
        |(p:Person {firstName: "Alice", lastName: "Foo"})
      """.stripMargin)

    // When
    val result = given.cypher("""
        |MATCH (a:Person)
        |RETURN a.firstName
      """.stripMargin)

    // Then
    result.records.data.toList should equal(List(Map("a.firstName" -> "Alice")))
  }

}
