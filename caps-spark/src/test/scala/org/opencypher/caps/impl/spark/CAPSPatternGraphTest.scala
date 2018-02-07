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

import org.apache.spark.sql.Row
import org.opencypher.caps.api.schema.Schema
import org.opencypher.caps.api.types._
import org.opencypher.caps.api.value.CypherValue._
import org.opencypher.caps.api.value.{CypherValue, _}
import org.opencypher.caps.impl.record._
import org.opencypher.caps.impl.syntax.RecordHeaderSyntax._
import org.opencypher.caps.ir.api.expr._
import org.opencypher.caps.ir.api.{Label, PropertyKey}
import org.opencypher.caps.ir.test._
import org.opencypher.caps.test.CAPSTestSuite
import org.opencypher.caps.test.fixture.GraphCreationFixture

import scala.collection.Bag
import scala.collection.JavaConverters._

class CAPSPatternGraphTest extends CAPSTestSuite with GraphCreationFixture {

  import CAPSGraphTestData._

  test("project pattern graph") {
    val inputGraph = initGraph(`:Person`)

    val person = inputGraph.cypher(
      """MATCH (a:Swedish)
        |RETURN GRAPH result OF (a)
      """.stripMargin)

    person.graphs("result").cypher("MATCH (n) RETURN n.name").records.iterator.toSet should equal(
      Set(
        CypherMap("n.name" -> "Mats")
      ))
  }

  test("project pattern graph with relationship") {
    val inputGraph = initGraph(`:Person` + `:KNOWS`)

    val person = inputGraph.cypher(
      """MATCH (a:Person:Swedish)-[r]->(b)
        |RETURN GRAPH result OF (a)-[r]->(b)
      """.stripMargin)

    person.graphs("result").cypher("MATCH (n) RETURN n.name").records.iterator.toSet should equal(
      Set(
        CypherMap("n.name" -> "Mats"),
        CypherMap("n.name" -> "Stefan"),
        CypherMap("n.name" -> "Martin"),
        CypherMap("n.name" -> "Max")
      ))
  }

  // TODO: Generate names for GRAPH OF pattern parts in frontend
  test("project pattern graph with created relationship") {
    val inputGraph = initGraph(`:Person` + `:KNOWS`)

    val person = inputGraph.cypher(
      """MATCH (a:Person:Swedish)-[r]->(b)
        |RETURN GRAPH result OF (a)-[foo:SWEDISH_KNOWS]->(b)
      """.stripMargin)

    person
      .graphs("result")
      .cypher("MATCH ()-[:SWEDISH_KNOWS]->(n) RETURN n.name")
      .records
      .iterator
      .toSet should equal(
      Set(
        CypherMap("n.name" -> "Stefan"),
        CypherMap("n.name" -> "Martin"),
        CypherMap("n.name" -> "Max")
      ))
  }

  test("project pattern graph with created node") {
    val inputGraph = initGraph(`:Person` + `:KNOWS`)

    val person = inputGraph.cypher(
      """MATCH (a:Person:Swedish)-[r]->(b)
        |RETURN GRAPH result OF (a)-[r]->(b)-[bar:KNOWS_A]->(baz)
      """.stripMargin)

    person
      .graphs("result")
      .cypher("MATCH (b)-[:KNOWS_A]->(n) WITH COUNT(n) as cnt RETURN cnt")
      .records
      .iterator
      .toSet should equal(
      Set(
        CypherMap("cnt" -> 3)
      ))
  }

  test("relationship scan for specific type") {
    val inputGraph = initGraph(`:KNOWS` + `:READS`)
    val inputRels = inputGraph.relationships("r")

    val patternGraph = CAPSGraph.create(inputRels, inputGraph.schema)
    val outputRels = patternGraph.relationships("r", CTRelationship("KNOWS"))

    outputRels.data.count() shouldBe 6
  }

  test("relationship scan for disjunction of types") {
    val inputGraph = initGraph(`:KNOWS` + `:READS` + `:INFLUENCES`)
    val inputRels = inputGraph.relationships("r")

    val patternGraph = CAPSGraph.create(inputRels, inputGraph.schema)
    val outputRels = patternGraph.relationships("r", CTRelationship("KNOWS", "INFLUENCES"))

    outputRels.data.count() shouldBe 7
  }

  test("relationship scan for all types") {
    val inputGraph = initGraph(`:KNOWS` + `:READS`)
    val inputRels = inputGraph.relationships("r")

    val patternGraph = CAPSGraph.create(inputRels, inputGraph.schema)
    val outputRels = patternGraph.relationships("r", CTRelationship)

    outputRels.data.count() shouldBe 10
  }

  test("project pattern graph with created node with labels") {
    val inputGraph = initGraph(`:Person` + `:KNOWS`)

    val person = inputGraph.cypher(
      """MATCH (a:Person:Swedish)-[r]->(b)
        |RETURN GRAPH result OF (a)-[r]->(b)-[bar:KNOWS_A]->(baz:Swede)
      """.stripMargin)

    val graph = person.graphs("result")

    graph.cypher("MATCH (n:Swede) RETURN labels(n)").records.iterator.toSet should equal(
      Set(
        CypherMap("labels(n)" -> List("Swede")),
        CypherMap("labels(n)" -> List("Swede")),
        CypherMap("labels(n)" -> List("Swede"))
      ))
  }

  //TODO: Test creating literal property value
  //TODO: Test creating computed property value

  test("Node scan from single node CAPSRecords") {
    val inputGraph = initGraph(`:Person`)
    val inputNodes = inputGraph.nodes("n")

    val patternGraph = CAPSGraph.create(inputNodes, inputGraph.schema)
    val outputNodes = patternGraph.nodes("n")

    outputNodes.toDF().columns should equal(
      Array(
        "n",
        "____n:Person",
        "____n:Swedish",
        "____n_dot_luckyNumberINTEGER",
        "____n_dot_nameSTRING"
      ))

    outputNodes.toDF().collect().toSet should equal(
      Set(
        Row(0L, true, true, 23L, "Mats"),
        Row(1L, true, false, 42L, "Martin"),
        Row(2L, true, false, 1337L, "Max"),
        Row(3L, true, false, 9L, "Stefan")
      ))
  }

  test("Node scan from mixed node CapsRecords") {
    val inputGraph = initGraph(`:Person` + `:Book`)
    val inputNodes = inputGraph.nodes("n")

    val patternGraph = CAPSGraph.create(inputNodes, inputGraph.schema)
    val outputNodes = patternGraph.nodes("n")

    outputNodes.toDF().columns.toSet should equal(
      Set(
        "n",
        "____n:Person",
        "____n:Swedish",
        "____n:Book",
        "____n_dot_nameSTRING",
        "____n_dot_luckyNumberINTEGER",
        "____n_dot_yearINTEGER",
        "____n_dot_titleSTRING"
      ))

    Bag(outputNodes.toDF().collect(): _*) should equal(
      Bag(
        Row(0L, false, true, true, 23L, "Mats", null, null),
        Row(1L, false, true, false, 42L, "Martin", null, null),
        Row(2L, false, true, false, 1337L, "Max", null, null),
        Row(3L, false, true, false, 9L, "Stefan", null, null),
        Row(4L, true, false, false, null, null, "1984", 1949L),
        Row(5L, true, false, false, null, null, "Cryptonomicon", 1999L),
        Row(6L, true, false, false, null, null, "The Eye of the World", 1990L),
        Row(7L, true, false, false, null, null, "The Circle", 2013L)
      ))
  }

  test("Node scan from multiple connected nodes") {
    val patternGraph = initPersonReadsBookGraph
    val outputNodes = patternGraph.nodes("n")

    outputNodes.toDF().columns should equal(
      Array(
        "n",
        "____n:Book",
        "____n:Person",
        "____n:Swedish",
        "____n_dot_luckyNumberINTEGER",
        "____n_dot_nameSTRING",
        "____n_dot_titleSTRING",
        "____n_dot_yearINTEGER"
      ))

    outputNodes.toDF().collect().toSet should equal(
      Set(
        Row(0L, false, true, true, 23L, "Mats", null, null),
        Row(1L, false, true, false, 42L, "Martin", null, null),
        Row(2L, false, true, false, 1337L, "Max", null, null),
        Row(3L, false, true, false, 9L, "Stefan", null, null),
        Row(4L, true, false, false, null, null, "1984", 1949L),
        Row(5L, true, false, false, null, null, "Cryptonomicon", 1999L),
        Row(6L, true, false, false, null, null, "The Eye of the World", 1990L),
        Row(7L, true, false, false, null, null, "The Circle", 2013L)
      ))
  }

  test("Specific node scan from multiple connected nodes") {
    val patternGraph = initPersonReadsBookGraph

    val outputNodes = patternGraph.nodes("n", CTNode("Person"))

    outputNodes.toDF().columns should equal(
      Array(
        "n",
        "____n:Person",
        "____n:Swedish",
        "____n_dot_luckyNumberINTEGER",
        "____n_dot_nameSTRING"
      ))

    outputNodes.toDF().collect().toSet should equal(
      Set(
        Row(0L, true, true, 23L, "Mats"),
        Row(1L, true, false, 42L, "Martin"),
        Row(2L, true, false, 1337L, "Max"),
        Row(3L, true, false, 9L, "Stefan")
      ))
  }

  test("Specific node scan from mixed node CapsRecords") {
    val inputGraph = initGraph(`:Person` + `:Book`)
    val inputNodes = inputGraph.nodes("n")

    val patternGraph = CAPSGraph.create(inputNodes, inputGraph.schema)
    val outputNodes = patternGraph.nodes("n", CTNode("Person"))

    outputNodes.toDF().columns should equal(
      Array(
        "n",
        "____n:Person",
        "____n:Swedish",
        "____n_dot_luckyNumberINTEGER",
        "____n_dot_nameSTRING"
      ))

    outputNodes.toDF().collect().toSet should equal(
      Set(
        Row(0L, true, true, 23L, "Mats"),
        Row(1L, true, false, 42L, "Martin"),
        Row(2L, true, false, 1337L, "Max"),
        Row(3L, true, false, 9L, "Stefan")
      ))
  }

  test("Node scan for missing label") {
    val inputGraph = initGraph(`:Book`)
    val inputNodes = inputGraph.nodes("n")

    val patternGraph = CAPSGraph.create(inputNodes, inputGraph.schema)

    patternGraph.nodes("n", CTNode("Person")).toDF().collect().toSet shouldBe empty
  }

  test("Supports .cypher node scans") {
    val patternGraph = initPersonReadsBookGraph

    patternGraph.cypher("MATCH (p:Person {name: 'Mats'}) RETURN p.luckyNumber").records.iterator.toBag should equal(
      Bag(CypherMap("p.luckyNumber" -> 23)))
  }

  test("Supports node scans from ad-hoc table") {
    val n: Var = 'n -> CTNode
    val fields = Seq(
      OpaqueField('p -> CTNode("Person")),
      OpaqueField(n),
      ProjectedExpr(HasLabel(n, Label("Person"))(CTBoolean)),
      OpaqueField('q -> CTNode("Foo"))
    )
    val (header, _) = TableHeader.empty.update(addContents(fields))

    val df = session.createDataFrame(
      List(
        Row(0L, 1L, true, 2L),
        Row(10L, 11L, false, 12L)
      ).asJava,
      CAPSRecordHeader.asSparkStructType(header))

    val schema = Schema.empty
      .withNodePropertyKeys("Person")()
      .withNodePropertyKeys("Foo")()

    val patternGraph = CAPSGraph.create(CAPSRecords.verifyAndCreate(header, df), schema)

    patternGraph.nodes("n", CTNode("Person")).iterator.toBag should equal(
      Bag(
        CypherMap("n" -> CAPSNode(0L, Set())),
        CypherMap("n" -> CAPSNode(1L, Set("Person"))),
        CypherMap("n" -> CAPSNode(10L, Set()))
      ))
  }

  ignore("Supports node scans from ad-hoc table 2") {
    val p: Var = 'p -> CTNode
    val c: Var = 'c -> CTNode
    val x: Var = 'x -> CTRelationship("IN")
    val fields = Seq(
      OpaqueField(c),
      ProjectedExpr(HasLabel(c, Label("Customer"))(CTBoolean)),
      ProjectedField('cName -> CTString.nullable, Property(c, PropertyKey("name"))(CTString.nullable)),
      OpaqueField(p),
      ProjectedExpr(HasLabel(p, Label("Person"))(CTBoolean)),
      ProjectedField('pName -> CTString.nullable, Property(p, PropertyKey("name"))(CTString)),
      ProjectedExpr(Property(p, PropertyKey("region"))(CTString)),
      ProjectedExpr(StartNode(x)(CTInteger)),
      ProjectedExpr(EndNode(x)(CTInteger)),
      OpaqueField(x),
      ProjectedExpr(Type(x)(CTString))
    )
    val (header, _) = TableHeader.empty.update(addContents(fields))

    val df = session.createDataFrame(
      List(
        Row(2001L, true, "Alice", 4L, true, "Alice", "US", 2001L, 4L, 4982162063360L, "IS"),
        Row(2002L, true, "Bob", 5L, true, "Bob", "US", 2002L, 5L, 4939212390400L, "IS"),
        Row(2008L, true, "Trudy", 11L, true, "Trudy", "EU", 2008L, 11L, 4947802324992L, "IS"),
        Row(2005L, true, "Carl", 8L, true, "Carl", "US", 2005L, 8L, 4698694221824L, "IS"),
        Row(2010L, true, "Oscar", 13L, true, "Oscar", "EU", 2010L, 13L, 5162550689792L, "IS"),
        Row(2011L, true, "Victor", 14L, true, "Victor", "EU", 2011L, 14L, 5076651343872L, "IS"),
        Row(2012L, true, "Peggy", 15L, true, "Peggy", "EU", 2012L, 15L, 5677946765312L, "IS"),
        Row(2006L, true, "Dave", 9L, true, "Dave", "US", 2006L, 9L, 5506148073472L, "IS"),
        Row(2009L, true, "Trent", 12L, true, "Trent", "EU", 2009L, 12L, 4466765987840L, "IS"),
        Row(2007L, true, "Mallory", 10L, true, "Mallory", "EU", 2007L, 10L, 5849745457152L, "IS"),
        Row(2003L, true, "Eve", 6L, true, "Eve", "US", 2003L, 6L, 5480378269696L, "IS"),
        Row(2004L, true, "Carol", 7L, true, "Carol", "US", 2004L, 7L, 5626407157760L, "IS")
      ).asJava,
      CAPSRecordHeader.asSparkStructType(header)
    )

    val schema = Schema.empty
      .withNodePropertyKeys("Person")("name" -> CTString, "region" -> CTString)
      .withNodePropertyKeys("Customer")("name" -> CTString.nullable)
      .withRelationshipType("IN")

    val patternGraph = CAPSGraph.create(CAPSRecords.verifyAndCreate(header, df), schema)

    //    patternGraph.nodes("n").toCypherMaps.collect().toSet should equal(Set(
    //      CypherMap("n" ->  0L),
    //      CypherMap("n" ->  1L),
    //      CypherMap("n" -> 10L)
    //    ))
  }

  test("Returns only distinct results") {
    val p = 'p -> CTNode("Person")
    val fields = Seq(
      OpaqueField(p),
      ProjectedExpr(HasLabel(p, Label("Person"))(CTBoolean)),
      ProjectedExpr(Property(p, PropertyKey("name"))(CTString))
    )
    val (header, _) = TableHeader.empty.update(addContents(fields))

    val sparkHeader = CAPSRecordHeader.asSparkStructType(header)
    val df = session.createDataFrame(
      List(
        Row(0L, true, "PersonPeter"),
        Row(0L, true, "PersonPeter")
      ).asJava,
      sparkHeader)

    val schema = Schema.empty
      .withNodePropertyKeys("Person")("name" -> CTString)

    val patternGraph = CAPSGraph.create(CAPSRecords.verifyAndCreate(header, df), schema)

    patternGraph.nodes("n", CTNode).iterator.toBag should equal(
      Bag(
        CypherMap("n" -> CAPSNode(0L, Set("Person"), CypherMap("name" -> "PersonPeter"))))
    )
  }

  test("Supports node scans when different variables have the same property keys") {
    val p = 'p -> CTNode("Person")
    val e = 'e -> CTNode("Employee")
    val fields = Seq(
      OpaqueField(p),
      ProjectedExpr(HasLabel(p, Label("Person"))(CTBoolean)),
      OpaqueField(e),
      ProjectedExpr(HasLabel(e, Label("Employee"))(CTBoolean)),
      ProjectedExpr(Property(p, PropertyKey("name"))(CTString)),
      ProjectedField('foo -> CTString, Property(e, PropertyKey("name"))(CTString))
    )
    val (header, _) = TableHeader.empty.update(addContents(fields))

    val sparkHeader = CAPSRecordHeader.asSparkStructType(header)
    val df = session.createDataFrame(
      List(
        Row(0L, true, 1L, true, "PersonPeter", "EmployeePeter"),
        Row(10L, true, 11L, true, "PersonSusanna", "EmployeeSusanna")
      ).asJava,
      sparkHeader)

    val schema = Schema.empty
      .withNodePropertyKeys("Person")("name" -> CTString)
      .withNodePropertyKeys("Employee")("name" -> CTString)

    val patternGraph = CAPSGraph.create(CAPSRecords.verifyAndCreate(header, df), schema)

    patternGraph.nodes("n", CTNode).iterator.toBag should equal(
      Bag(
        CypherMap("n" -> CAPSNode(0L, Set("Person"), CypherMap("name" -> "PersonPeter"))),
        CypherMap("n" -> CAPSNode(1L, Set("Employee"), CypherMap("name" -> "EmployeePeter"))),
        CypherMap("n" -> CAPSNode(10L, Set("Person"), CypherMap("name" -> "PersonSusanna"))),
        CypherMap("n" -> CAPSNode(11L, Set("Employee"), CypherMap("name" -> "EmployeeSusanna")))
      )
    )
  }

  //TODO: Requires changes to schema verification
  ignore("Supports node scans when variables have the same label and property") {
    val p = 'p -> CTNode("Person")
    val e = 'e -> CTNode("Employee")
    val pe = 'pe -> CTNode("Person", "Employee")
    val fields = Seq(
      OpaqueField(p),
      OpaqueField(e),
      OpaqueField(pe),
      ProjectedExpr(Property(p, PropertyKey("name"))(CTString)),
      ProjectedExpr(Property(e, PropertyKey("name"))(CTString.nullable)),
      ProjectedExpr(Property(pe, PropertyKey("name"))(CTString))
    )
    val (header, _) = TableHeader.empty.update(addContents(fields))

    val sparkHeader = CAPSRecordHeader.asSparkStructType(header)
    val df = session.createDataFrame(
      List(
        Row(0L, 1L, 2L, "PersonPeter", "EmployeePeter", "HybridPeter"),
        Row(10L, 11L, 12L, "person.graphsusanna", null, "HybridSusanna")
      ).asJava,
      sparkHeader)

    val schema = Schema.empty
      .withNodePropertyKeys("Person")("name" -> CTString)
      .withNodePropertyKeys("Employee")("name" -> CTString.nullable)
      .withNodePropertyKeys("Employee", "Person")("name" -> CTString)

    val patternGraph = CAPSGraph.create(CAPSRecords.verifyAndCreate(header, df), schema)

    patternGraph.nodes("n", CTNode).toMaps should equal(
      Bag(
        CypherMap("n" -> 0L, "n.name" -> "PersonPeter", "n:Person" -> true, "n:Employee" -> false),
        CypherMap("n" -> 1L, "n.name" -> "EmployeePeter", "n:Person" -> false, "n:Employee" -> true),
        CypherMap("n" -> 2L, "n.name" -> "HybridPeter", "n:Person" -> true, "n:Employee" -> true),
        CypherMap("n" -> 10L, "n.name" -> "person.graphsusanna", "n:Person" -> true, "n:Employee" -> false),
        CypherMap("n" -> 11L, "n.name" -> null, "n:Person" -> false, "n:Employee" -> true),
        CypherMap("n" -> 12L, "n.name" -> "HybridSusanna", "n:Person" -> true, "n:Employee" -> true)
      ))
  }

  test("Reduce cardinality of the pattern graph base table") {
    val given = initGraph(
      """
        |CREATE (a: Person)
        |CREATE (b: Person)
        |CREATE (a)-[:HAS_INTEREST]->(i1:Interest {val: 1})
        |CREATE (a)-[:HAS_INTEREST]->(i2:Interest {val: 2})
        |CREATE (a)-[:HAS_INTEREST]->(i3:Interest {val: 3})
        |CREATE (a)-[:KNOWS]->(b)
      """.stripMargin)

    val when = given.cypher(
      """
        |MATCH (i:Interest)<-[h:HAS_INTEREST]-(a:Person)-[k:KNOWS]->(b:Person)
        |RETURN GRAPH result OF (a)-[k]->(b)
      """.stripMargin)

    when.graphs("result").asInstanceOf[CAPSPatternGraph].baseTable.data.count() should equal(3)
  }

  test("deduplicating identical instances of the same graph of pattern") {
    val given = initGraph(
      """
        |CREATE (a: Person)
        |CREATE (b: Person)
        |CREATE (a)-[:HAS_INTEREST]->(i1:Interest {val: 1})
        |CREATE (a)-[:HAS_INTEREST]->(i2:Interest {val: 2})
        |CREATE (a)-[:HAS_INTEREST]->(i3:Interest {val: 3})
        |CREATE (a)-[:KNOWS]->(b)
      """.stripMargin)

    val when = given.cypher(
      """
        |MATCH (i:Interest)<-[h:HAS_INTEREST]-(a:Person)-[k:KNOWS]->(b:Person)
        |RETURN GRAPH result OF (a)-[f:FOO]->(b)
      """.stripMargin)

    when.graphs("result").relationships("f", CTRelationship("FOO")).size should equal(3)
  }

  private def initPersonReadsBookGraph: CAPSGraph = {
    val inputGraph = initGraph(`:Person` + `:Book` + `:READS`)

    val books = inputGraph.nodes("b", CTNode("Book"))
    val booksDf = books.toDF().as("b")
    val reads = inputGraph.relationships("r", CTRelationship("READS"))
    val readsDf = reads.toDF().as("r")
    val persons = inputGraph.nodes("p", CTNode("Person"))
    val personsDf = persons.toDF().as("p")

    val joinedDf = personsDf
      .join(readsDf, personsDf.col("p") === readsDf.col("____source(r)"))
      .join(booksDf, readsDf.col("____target(r)") === booksDf.col("b"))

    val slots = persons.header.slots ++ reads.header.slots ++ books.header.slots
    val joinHeader = TableHeader.from(slots.map(_.content): _*)

    CAPSGraph.create(CAPSRecords.verifyAndCreate(joinHeader, joinedDf), inputGraph.schema)
  }
}
