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
package org.opencypher.graphddl

import org.opencypher.okapi.api.graph.GraphName
import org.opencypher.okapi.api.schema.{Schema, SchemaPattern}
import org.opencypher.okapi.api.types.{CTFloat, CTInteger, CTString}
import org.scalatest.{FunSpec, Matchers}

class GraphDdlTest extends FunSpec with Matchers {

  val ddlString: String =
    s"""
       |SET SCHEMA dataSourceName.fooDatabaseName
       |
       |CREATE GRAPH TYPE fooSchema (
       | Person ( name STRING, age INTEGER ),
       | Book   ( title STRING ) ,
       | READS  ( rating FLOAT ) ,
       | (Person),
       | (Book),
       | (Person)-[READS]->(Book)
       |)
       |CREATE GRAPH fooGraph OF fooSchema (
       |  (Person) FROM personView1 ( person_name1 AS name )
       |           FROM personView2 ( person_name2 AS name ),
       |  (Book)   FROM bookView    ( book_title AS title ),
       |
       |  (Person)-[READS]->(Book)
       |    FROM readsView1 e ( value1 AS rating )
       |      START NODES (Person) FROM personView1 p JOIN ON p.person_id1 = e.person
       |      END   NODES (Book)   FROM bookView    b JOIN ON e.book       = b.book_id
       |    FROM readsView2 e ( value2 AS rating )
       |      START NODES (Person) FROM personView2 p JOIN ON p.person_id2 = e.person
       |      END   NODES (Book)   FROM bookView    b JOIN ON e.book       = b.book_id
       |)
     """.stripMargin

  it("converts to GraphDDL IR") {


    val graphDdl = GraphDdl(ddlString)

    val personKey1 = NodeViewKey(NodeType("Person"), ViewId(None, List("dataSourceName", "fooDatabaseName", "personView1")))
    val personKey2 = NodeViewKey(NodeType("Person"), ViewId(None, List("dataSourceName", "fooDatabaseName", "personView2")))
    val bookKey    = NodeViewKey(NodeType("Book"),   ViewId(None, List("dataSourceName", "fooDatabaseName", "bookView")))
    val readsKey1  = EdgeViewKey(RelationshipType("Person", "READS", "Book"),  ViewId(None, List("dataSourceName", "fooDatabaseName", "readsView1")))
    val readsKey2  = EdgeViewKey(RelationshipType("Person", "READS", "Book"),  ViewId(None, List("dataSourceName", "fooDatabaseName", "readsView2")))

    val expected = GraphDdl(
      Map(
        GraphName("fooGraph") -> Graph(GraphName("fooGraph"),
          Schema.empty
            .withNodePropertyKeys("Person")("name" -> CTString, "age" -> CTInteger)
            .withNodePropertyKeys("Book")("title" -> CTString)
            .withRelationshipPropertyKeys("READS")("rating" -> CTFloat)
            .withSchemaPatterns(SchemaPattern("Person", "READS", "Book")),
          Map(
            personKey1 -> NodeToViewMapping(
              nodeType = NodeType("Person"),
              view = personKey1.viewId,
              propertyMappings = Map("name" -> "person_name1", "age" -> "age")),
            personKey2 -> NodeToViewMapping(
              nodeType = NodeType("Person"),
              view = personKey2.viewId,
              propertyMappings = Map("name" -> "person_name2", "age" -> "age")),
            bookKey -> NodeToViewMapping(
              nodeType = NodeType("Book"),
              view = bookKey.viewId,
              propertyMappings = Map("title" -> "book_title"))
          ),
          List(
            EdgeToViewMapping(
              relType = RelationshipType("Person", "READS", "Book"),
              view = readsKey1.viewId,
              startNode = StartNode(personKey1, List(Join("person_id1", "person"))),
              endNode = EndNode(bookKey, List(Join("book_id", "book"))),
              propertyMappings = Map("rating" -> "value1")),
            EdgeToViewMapping(
              relType = RelationshipType("Person", "READS", "Book"),
              view = readsKey2.viewId,
              startNode = StartNode(personKey2, List(Join("person_id2", "person"))),
              endNode = EndNode(bookKey, List(Join("book_id", "book"))),
              propertyMappings = Map("rating" -> "value2"))
          )
        )
      )
    )

    graphDdl shouldEqual expected
  }

  it("extracts join keys for a given node view key in start node position") {
    val maybeJoinColumns = GraphDdl(ddlString).graphs(GraphName("fooGraph"))
      .nodeIdColumnsFor(NodeViewKey(NodeType("Person"), ViewId(None, List("dataSourceName", "fooDatabaseName", "personView1"))))

    maybeJoinColumns shouldEqual Some(List("person_id1"))
  }

  it("extracts join keys for a given node view key in end node position") {
    val maybeJoinColumns = GraphDdl(ddlString).graphs(GraphName("fooGraph"))
      .nodeIdColumnsFor(NodeViewKey(NodeType("Book"), ViewId(None, List("dataSourceName", "fooDatabaseName", "bookView"))))

    maybeJoinColumns shouldEqual Some(List("book_id"))
  }

  it("does not extract join keys for an invalid node view key") {
    val maybeJoinColumns = GraphDdl(ddlString).graphs(GraphName("fooGraph"))
      .nodeIdColumnsFor(NodeViewKey(NodeType("A"), ViewId(None, List("dataSourceName", "fooDatabaseName", "A"))))

    maybeJoinColumns shouldEqual None
  }

  it("allows SET SCHEMA and fully qualified names") {
    val ddl = GraphDdl(
      """SET SCHEMA ds1.db1
        |
        |CREATE GRAPH TYPE fooSchema (
        |  Person,
        |  Account,
        |  (Person),
        |  (Account)
        |)
        |CREATE GRAPH fooGraph OF fooSchema (
        |  (Person)  FROM personView,
        |  (Account) FROM ds2.db2.accountView
        |)
      """.stripMargin)

    ddl.graphs(GraphName("fooGraph")).nodeToViewMappings.keys shouldEqual Set(
      NodeViewKey(NodeType("Person"), ViewId(None, List("ds1", "db1", "personView"))),
      NodeViewKey(NodeType("Account"), ViewId(None, List("ds2", "db2", "accountView")))
    )
  }

  it("allows compact inline graph definition") {
    val ddl = GraphDdl(
      """SET SCHEMA ds1.db1
        |CREATE GRAPH myGraph (
        |  A (x STRING), B (y STRING),
        |  (A)-[B]->(A),
        |  (A) FROM a,
        |  (A)-[B]->(A) FROM b e
        |    START NODES (A) FROM a n JOIN ON e.id = n.id
        |    END   NODES (A) FROM a n JOIN ON e.id = n.id
        |)
      """.stripMargin)

    val A_a = NodeViewKey(NodeType("A"), ViewId(None, List("ds1", "db1", "a")))

    ddl.graphs(GraphName("myGraph")) shouldEqual Graph(
      name = GraphName("myGraph"),
      graphType = Schema.empty
        .withNodePropertyKeys("A")("x" -> CTString)
        .withRelationshipPropertyKeys("B")("y" -> CTString)
        .withSchemaPatterns(SchemaPattern("A", "B", "A")),
      nodeToViewMappings = Map(
        A_a -> NodeToViewMapping(NodeType("A"), ViewId(None, List("ds1", "db1", "a")), Map("x" -> "x"))
      ),
      edgeToViewMappings = List(
        EdgeToViewMapping(RelationshipType("A", "B", "A"), ViewId(None, List("ds1", "db1", "b")),
          StartNode(A_a, List(Join("id", "id"))),
          EndNode(A_a, List(Join("id", "id"))),
          Map("y" -> "y")
        )
      )
    )
  }

  it("allows these equivalent graph definitions") {
    val ddls = List(
      // most compact form
      GraphDdl("""SET SCHEMA ds1.db1
                 |CREATE GRAPH myGraph (
                 |  A (x STRING), B (y STRING),
                 |  (A) FROM a,
                 |  (A)-[B]->(A) FROM b e
                 |    START NODES (A) FROM a n JOIN ON e.id = n.id
                 |    END   NODES (A) FROM a n JOIN ON e.id = n.id
                 |)
               """.stripMargin),
      // mixed order
      GraphDdl("""SET SCHEMA ds1.db1
                 |CREATE GRAPH myGraph (
                 |  (A)-[B]->(A) FROM b e
                 |    START NODES (A) FROM a n JOIN ON e.id = n.id
                 |    END   NODES (A) FROM a n JOIN ON e.id = n.id,
                 |  A (x STRING), B (y STRING),
                 |  (A) FROM a
                 |)
               """.stripMargin),
      // explicit node and rel type definition
      GraphDdl("""SET SCHEMA ds1.db1
                 |CREATE GRAPH myGraph (
                 |  A (x STRING), B (y STRING),
                 |  (A), (A)-[B]->(A),
                 |  (A) FROM a,
                 |  (A)-[B]->(A) FROM b e
                 |    START NODES (A) FROM a n JOIN ON e.id = n.id
                 |    END   NODES (A) FROM a n JOIN ON e.id = n.id
                 |)
               """.stripMargin),
      // pure type definitions extracted to graph type
      GraphDdl("""SET SCHEMA ds1.db1
                 |CREATE GRAPH TYPE myType (
                 |  A (x STRING), B (y STRING),
                 |  (A), (A)-[B]->(A)
                 |)
                 |CREATE GRAPH myGraph OF myType (
                 |  (A) FROM a,
                 |  (A)-[B]->(A) FROM b e
                 |    START NODES (A) FROM a n JOIN ON e.id = n.id
                 |    END   NODES (A) FROM a n JOIN ON e.id = n.id
                 |)
               """.stripMargin),
      // shadowing
      GraphDdl("""SET SCHEMA ds1.db1
                 |CREATE GRAPH TYPE myType (
                 |  A (x STRING), B (foo STRING),
                 |  (A), (A)-[B]->(A)
                 |)
                 |CREATE GRAPH myGraph OF myType (
                 |  B (y STRING),
                 |  (A) FROM a,
                 |  (A)-[B]->(A) FROM b e
                 |    START NODES (A) FROM a n JOIN ON e.id = n.id
                 |    END   NODES (A) FROM a n JOIN ON e.id = n.id
                 |)
               """.stripMargin),
      // only label types in graph type
      GraphDdl("""SET SCHEMA ds1.db1
                 |CREATE GRAPH TYPE myType (
                 |  A (x STRING), B (foo STRING)
                 |)
                 |CREATE GRAPH myGraph OF myType (
                 |  B (y STRING),
                 |  (A) FROM a,
                 |  (A)-[B]->(A) FROM b e
                 |    START NODES (A) FROM a n JOIN ON e.id = n.id
                 |    END   NODES (A) FROM a n JOIN ON e.id = n.id
                 |)
               """.stripMargin)
    )

    ddls(1) shouldEqual ddls.head
    ddls(2) shouldEqual ddls.head
    ddls(3) shouldEqual ddls.head
    ddls(4) shouldEqual ddls.head
    ddls(5) shouldEqual ddls.head
  }

  it("allows these equivalent graph type definitions") {
    val ddls = List(
      // most compact form
      GraphDdl("""
                 |CREATE GRAPH TYPE myType (
                 |  A (x STRING), B (y STRING), C (z STRING),
                 |  (A)-[B]->(C)
                 |)
                 |CREATE GRAPH myGraph OF myType ()
               """.stripMargin),
      // explicit node and rel type definitions
      GraphDdl("""CREATE GRAPH TYPE myType (
                 |  A (x STRING), B (y STRING), C (z STRING),
                 |  (A), (C),
                 |  (A)-[B]->(C)
                 |)
                 |CREATE GRAPH myGraph OF myType ()
               """.stripMargin),
      // implicit node type definitions
      GraphDdl("""CREATE GRAPH TYPE myType (
                 |  A (x STRING), B (y STRING), C (z STRING),
                 |  (A)-[B]->(C)
                 |)
                 |CREATE GRAPH myGraph OF myType ()
               """.stripMargin),
      // shadowing
      GraphDdl("""CREATE ELEMENT TYPE A (foo STRING)
                 |CREATE GRAPH TYPE myType (
                 |  A (x STRING), B (y STRING), C (z STRING),
                 |  (A)-[B]->(C)
                 |)
                 |CREATE GRAPH myGraph OF myType ()
               """.stripMargin)
    )

    ddls(1) shouldEqual ddls.head
    ddls(2) shouldEqual ddls.head
    ddls(3) shouldEqual ddls.head
  }

  it("fails on duplicate node mappings") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |SET SCHEMA db.schema
      |
      |CREATE GRAPH TYPE fooSchema (
      | Person,
      | (Person)
      |)
      |CREATE GRAPH fooGraph OF fooSchema (
      |  (Person) FROM personView
      |           FROM personView
      |)
    """.stripMargin)
    e.getFullMessage should (include("fooGraph") and include("(Person)") and include("db.schema.personView"))
  }

  it("fails on duplicate relationship mappings") {
    val e = the[GraphDdlException] thrownBy GraphDdl(
      """
        |SET SCHEMA db.schema
        |
        |CREATE GRAPH TYPE fooSchema (
        | Person,
        | KNOWS,
        | (Person)-[KNOWS]->(Person)
        |)
        |CREATE GRAPH fooGraph OF fooSchema (
        | (Person)-[KNOWS]->(Person)
        |   FROM pkpView e
        |     START NODES (Person) FROM a n JOIN ON e.id = n.id
        |     END   NODES (Person) FROM a n JOIN ON e.id = n.id,
        |   FROM pkpView e
        |     START NODES (Person) FROM a n JOIN ON e.id = n.id
        |     END   NODES (Person) FROM a n JOIN ON e.id = n.id
        |)
      """.stripMargin)
    e.getFullMessage should (include("fooGraph") and include("(Person)-[KNOWS]->(Person)") and include("db.schema.pkpView"))
  }

  it("fails on duplicate global labels") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |CREATE ELEMENT TYPE Person
      |CREATE ELEMENT TYPE Person
    """.stripMargin)
    e.getFullMessage should include("Person")
  }

  it("fails on duplicate local labels") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |CREATE GRAPH TYPE fooSchema (
      | Person,
      | Person
      |)
    """.stripMargin)
    e.getFullMessage should (include("fooSchema") and include("Person"))
  }

  it("fails on duplicate graph types") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |CREATE GRAPH TYPE fooSchema ()
      |CREATE GRAPH TYPE fooSchema ()
    """.stripMargin)
    e.getFullMessage should include("fooSchema")
  }

  it("fails on duplicate graphs") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |CREATE GRAPH TYPE fooSchema ()
      |CREATE GRAPH fooGraph OF fooSchema ()
      |CREATE GRAPH fooGraph OF fooSchema ()
    """.stripMargin)
    e.getFullMessage should include("fooGraph")
  }

  it("fails on unresolved graph type") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |CREATE GRAPH TYPE fooSchema ()
      |CREATE GRAPH fooGraph OF barSchema ()
    """.stripMargin)
    e.getFullMessage should (include("fooGraph") and include("fooSchema") and include("barSchema"))
  }

  it("fails on unresolved labels") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |CREATE GRAPH TYPE fooSchema (
      | Person1,
      | Person2,
      | (Person3, Person4)
      |)
    """.stripMargin)
    e.getFullMessage should (include("fooSchema") and include("Person3") and include("Person4"))
  }

  it("fails on unresolved labels in mapping") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |CREATE GRAPH fooGraph (
      | Person1,
      | Person2,
      | (Person3, Person4) FROM x
      |)
    """.stripMargin)
    e.getFullMessage should (include("fooGraph") and include("Person3") and include("Person4"))
  }

  it("fails on incompatible property types") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |CREATE GRAPH TYPE fooSchema (
      | Person1 ( age STRING ) ,
      | Person2 ( age INTEGER ) ,
      | (Person1, Person2)
      |)
    """.stripMargin)
    e.getFullMessage should (
      include("fooSchema") and include("Person1") and include("Person2") and include("age") and include("STRING")  and include("INTEGER")
    )
  }

  it("fails on unresolved property names") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |SET SCHEMA a.b
      |CREATE GRAPH TYPE fooSchema (
      | Person ( age1 STRING ) ,
      | (Person)
      |)
      |CREATE GRAPH fooGraph OF fooSchema (
      |  (Person) FROM personView ( person_name AS age2 )
      |)
    """.stripMargin)
    e.getFullMessage should (
      include("fooGraph") and include("Person") and include("personView") and include("age1")  and include("age2")
    )
  }

  it("fails on missing set schema statement") {
    val e = the [GraphDdlException] thrownBy GraphDdl("""
      |CREATE GRAPH TYPE fooSchema (
      | Person,
      | (Person)
      |)
      |CREATE GRAPH fooGraph OF fooSchema (
      |  (Person) FROM personView
      |)
    """.stripMargin)
    e.getFullMessage should (
      include("fooGraph") and include("personView")
    )
  }

}
