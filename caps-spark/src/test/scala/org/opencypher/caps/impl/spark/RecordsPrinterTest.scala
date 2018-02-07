/*
 * Copyright (c) 2016-2018 "Neo4j, Inc." [https://neo4j.com]
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
 */
package org.opencypher.caps.impl.spark

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.charset.StandardCharsets.UTF_8

import org.opencypher.caps.api.types.CTNode
import org.opencypher.caps.impl.record.{CypherTable, OpaqueField, TableHeader}
import org.opencypher.caps.impl.syntax.RecordHeaderSyntax._
import org.opencypher.caps.impl.util.PrintOptions
import org.opencypher.caps.ir.api.expr.Var
import org.opencypher.caps.test.CAPSTestSuite
import org.opencypher.caps.test.fixture.GraphCreationFixture

class RecordsPrinterTest extends CAPSTestSuite with GraphCreationFixture {

  implicit val options: PrintOptions = PrintOptions.out

  test("unit table") {
    // Given
    val records = CAPSRecords.unit()

    // When
    print(records)

    // Then
    val result = getString
    result should equal(
      """+----------------------+
        !| (no columns)         |
        !+----------------------+
        !| (empty row)          |
        !+----------------------+
        !(1 rows)
        !""".stripMargin('!')
    )
  }

  test("single column, no rows") {
    // Given
    val records = CAPSRecords.empty(headerOf('foo))

    // When
    print(records)

    // Then
    getString should equal(
      """+----------------------+
        !| foo                  |
        !+----------------------+
        !(no rows)
        !""".stripMargin('!')
    )
  }

  test("single column, three rows") {
    // Given
    val records = CAPSRecords.create(Seq(Row1("myString"), Row1("foo"), Row1(null)))

    // When
    print(records)

    // Then
    val result = getString
    result should equal(
      """+----------------------+
        !| foo                  |
        !+----------------------+
        !| 'myString'           |
        !| 'foo'                |
        !| null                 |
        !+----------------------+
        !(3 rows)
        !""".stripMargin('!')
    )
  }

  test("three columns, three rows") {
    // Given
    val records = CAPSRecords.create(
      Seq(
        Row3("myString", 4L, false),
        Row3("foo", 99999999L, true),
        Row3(null, -1L, true)
      ))

    // When
    print(records)

    // Then
    getString should equal(
      """+--------------------------------------------------------------------+
        !| foo                  | v                    | veryLongColumnNameWi |
        !+--------------------------------------------------------------------+
        !| 'myString'           | 4                    | false                |
        !| 'foo'                | 99999999             | true                 |
        !| null                 | -1                   | true                 |
        !+--------------------------------------------------------------------+
        !(3 rows)
        !""".stripMargin('!')
    )
  }

  test("return property values without alias") {
    val given =
      initGraph("""
        |CREATE (a:Person {name: "Alice"})-[:LIVES_IN]->(city:City)<-[:LIVES_IN]-(b:Person {name: "Bob"})
      """.stripMargin)

    val when = given.cypher("""MATCH (a:Person)-[:LIVES_IN]->(city:City)<-[:LIVES_IN]-(b:Person)
        |RETURN a.name, b.name
        |ORDER BY a.name
      """.stripMargin)

    print(when.records)

    getString should equal("""+---------------------------------------------+
        !| a.name               | b.name               |
        !+---------------------------------------------+
        !| 'Alice'              | 'Bob'                |
        !| 'Bob'                | 'Alice'              |
        !+---------------------------------------------+
        !(2 rows)
        !""".stripMargin('!'))
  }

  var baos: ByteArrayOutputStream = _

  override def beforeEach(): Unit = {
    baos = new ByteArrayOutputStream()
  }

  private case class Row1(foo: String)
  private case class Row3(foo: String, v: Long, veryLongColumnNameWithBoolean: Boolean)

  private def headerOf(fields: Symbol*): TableHeader = {
    val value1 = fields.map(f => OpaqueField(Var(f.name)(CTNode)))
    val (header, _) = TableHeader.empty.update(addContents(value1))
    header
  }

  private def getString =
    new String(baos.toByteArray, UTF_8)

  private def print(r: CypherTable)(implicit options: PrintOptions): Unit =
    RecordsPrinter.print(r)(options.stream(new PrintStream(baos, true, UTF_8.name())))
}
