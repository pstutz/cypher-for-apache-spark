/*
 * Copyright (c) 2016-2019 "Neo4j Sweden, AB" [https://neo4j.com]
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
package org.opencypher.spark.api.io.sql

import org.apache.spark.sql.types.{IntegerType, LongType, StructField, StructType}
import org.apache.spark.sql.{Row, SaveMode}
import org.opencypher.graphddl.GraphDdl
import org.opencypher.okapi.api.graph.GraphName
import org.opencypher.okapi.api.value.CypherValue.CypherMap
import org.opencypher.okapi.testing.Bag
import org.opencypher.spark.api.io.FileFormat
import org.opencypher.spark.api.io.sql.SqlDataSourceConfig.{File, Hive, Jdbc}
import org.opencypher.spark.api.value.{CAPSNode, CAPSRelationship}
import org.opencypher.spark.impl.CAPSFunctions.{partitioned_id_assignment, rowIdSpaceBitsUsedByMonotonicallyIncreasingId}
import org.opencypher.spark.testing.CAPSTestSuite
import org.opencypher.spark.testing.fixture.{H2Fixture, HiveFixture}

import scala.collection.JavaConverters._

class SqlPropertyGraphDataSourceTest extends CAPSTestSuite with HiveFixture with H2Fixture {

  private val dataSourceName = "fooDataSource"
  private val databaseName = "fooDatabase"
  private val fooGraphName = GraphName("fooGraph")

  private def computePartitionedRowId(rowIndex: Long, partitionStartDelta: Long): Long = {
    (partitionStartDelta << rowIdSpaceBitsUsedByMonotonicallyIncreasingId) + rowIndex
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createHiveDatabase(databaseName)
  }

  override protected def afterAll(): Unit = {
    dropHiveDatabase(databaseName)
    super.afterAll()
  }

  it("adds deltas to generated ids") {
    import sparkSession.implicits._
    val df = sparkSession.createDataFrame(Seq(Tuple1("A"), Tuple1("B"), Tuple1("C"))).toDF("alphabet")
    val withIds = df.withColumn("id", partitioned_id_assignment(0))
    val vanillaIds = List(0, 1, 2)
    withIds.select("id").collect().map(row => row.get(0)).toList should equal(vanillaIds)
    val idsWithDeltaAdded = df.withColumn("id", partitioned_id_assignment(2))
    val resultWithDelta = idsWithDeltaAdded.select("id").collect().map(row => row.get(0))
    resultWithDelta should equal(vanillaIds.map(computePartitionedRowId(_, 2)))
    resultWithDelta should equal(List(0x400000000L, 0x400000001L, 0x400000002L))

    val largeDf = sparkSession.sparkContext.parallelize(
      Seq.fill(100) {
        Tuple1("foo")
      }, 100
    ).toDF("fooCol")
    val largeDfWithIds = largeDf.withColumn("id", partitioned_id_assignment(100))
    val largeResultWithDelta = largeDfWithIds.select("id").collect().map(row => row.get(0).asInstanceOf[Long]).map(_ >> 33).sorted.toList
    val expectation = (0L until 100L).map(rowIndex => computePartitionedRowId(rowIndex, 100L + rowIndex)).map(_ >> 33).sorted.toList
    largeResultWithDelta should equal(expectation)
  }

  it("reads nodes from a table") {
    val fooView = "foo_view"

    val ddlString =
      s"""
         |SET SCHEMA $dataSourceName.$databaseName
         |
         |CREATE GRAPH TYPE fooSchema (
         | Foo ( foo STRING ) ,
         | (Foo)
         |)
         |CREATE GRAPH fooGraph OF fooSchema (
         |  (Foo) FROM $fooView
         |)
     """.stripMargin

    sparkSession
      .createDataFrame(Seq(Tuple1("Alice")))
      .toDF("foo")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$fooView")

    val ds = SqlPropertyGraphDataSource(GraphDdl(ddlString), Map(dataSourceName -> Hive))

    ds.graph(fooGraphName).nodes("n").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n" -> CAPSNode(0, Set("Foo"), CypherMap("foo" -> "Alice")))
    ))
  }

  it("reads nodes from a table with custom column mapping") {
    val fooView = "foo_view"

    val ddlString =
      s"""
         |SET SCHEMA $dataSourceName.$databaseName
         |
         |CREATE GRAPH TYPE fooSchema (
         | Foo ( key1 INTEGER, key2 String ),
         | (Foo)
         |)
         |
         |CREATE GRAPH fooGraph OF fooSchema (
         |  (Foo) FROM $fooView (col1 AS key2, col2 AS key1)
         |)
     """.stripMargin

    sparkSession
      .createDataFrame(Seq(Tuple2("Alice", 42L)))
      .toDF("col1", "col2")
      .write.mode(SaveMode.Overwrite).mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$fooView")

    val ds = SqlPropertyGraphDataSource(GraphDdl(ddlString), Map(dataSourceName -> Hive))

    ds.graph(fooGraphName).nodes("n").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n" -> CAPSNode(0, Set("Foo"), CypherMap("key1" -> 42L, "key2" -> "Alice")))
    ))
  }

  it("reads nodes from multiple tables") {
    val fooView = "foo_view"
    val barView = "bar_view"

    val ddlString =
      s"""
         |SET SCHEMA $dataSourceName.$databaseName
         |
         |CREATE GRAPH TYPE fooSchema (
         | Foo ( foo STRING ) ,
         | Bar ( bar INTEGER ) ,
         | (Foo),
         | (Bar)
         |)
         |
         |CREATE GRAPH fooGraph OF fooSchema (
         |  (Foo) FROM $fooView,
         |  (Bar) FROM $barView
         |)
     """.stripMargin

    sparkSession
      .createDataFrame(Seq(Tuple1("Alice")))
      .toDF("foo")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$fooView")
    sparkSession
      .createDataFrame(Seq(Tuple1(0L)))
      .toDF("bar")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$barView")

    val ds = SqlPropertyGraphDataSource(GraphDdl(ddlString), Map(dataSourceName -> Hive))

    ds.graph(fooGraphName).nodes("n").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n" -> CAPSNode(computePartitionedRowId(rowIndex = 0, partitionStartDelta = 0), Set("Foo"), CypherMap("foo" -> "Alice"))),
      CypherMap("n" -> CAPSNode(computePartitionedRowId(rowIndex = 0, partitionStartDelta = 1), Set("Bar"), CypherMap("bar" -> 0L)))
    ))
  }

  it("reads relationships from a table") {
    val personView = "person_view"
    val bookView = "bookView_view"
    val readsView = "reads_view"

    val ddlString =
      s"""
         |SET SCHEMA $dataSourceName.$databaseName
         |
         |CREATE GRAPH TYPE fooSchema (
         | Person ( name STRING ) ,
         | Book   ( title STRING ) ,
         | READS  ( rating FLOAT ) ,
         | (Person),
         | (Book),
         | (Person)-[READS]->(Book)
         |)
         |
         |CREATE GRAPH fooGraph OF fooSchema (
         |  (Person) FROM $personView ( person_name AS name ),
         |  (Book)   FROM $bookView (book_title AS title ),
         |  (Person)-[READS]->(Book)
         |    FROM $readsView edge
         |      START NODES (Person) FROM $personView alias_person JOIN ON alias_person.person_id = edge.person
         |      END NODES   (Book)   FROM $bookView   alias_book   JOIN ON edge.book = alias_book.book_id
         |)
     """.stripMargin

    sparkSession
      .createDataFrame(Seq((0L, "Alice")))
      .toDF("person_id", "person_name")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$personView")
    sparkSession
      .createDataFrame(Seq((1L, "1984")))
      .toDF("book_id", "book_title")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$bookView")
    sparkSession
      .createDataFrame(Seq((0L, 1L, 42.23)))
      .toDF("person", "book", "rating")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$readsView")

    val ds = SqlPropertyGraphDataSource(GraphDdl(ddlString), Map(dataSourceName -> Hive))

    val personId = computePartitionedRowId(rowIndex = 0, partitionStartDelta = 0)
    val bookId = computePartitionedRowId(rowIndex = 0, partitionStartDelta = 1)

    ds.graph(fooGraphName).nodes("n").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n" -> CAPSNode(personId, Set("Person"), CypherMap("name" -> "Alice"))),
      CypherMap("n" -> CAPSNode(bookId, Set("Book"), CypherMap("title" -> "1984")))
    ))

    ds.graph(fooGraphName).relationships("r").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("r" -> CAPSRelationship(
        id = computePartitionedRowId(rowIndex = 0, partitionStartDelta = 0),
        startId = personId,
        endId = bookId,
        relType = "READS",
        properties = CypherMap("rating" -> 42.23)))
    ))
  }

  it("reads relationships from a table with colliding column names") {
    val nodesView = "nodes_view"
    val relsView = "rels_view"

    val ddlString =
      s"""
         |SET SCHEMA $dataSourceName.$databaseName
         |
         |CREATE GRAPH TYPE fooSchema (
         | Node ( id INTEGER, start STRING, end STRING ),
         | REL  ( id INTEGER, start STRING, end STRING ),
         | (Node),
         | (Node)-[REL]->(Node)
         |)
         |CREATE GRAPH fooGraph OF fooSchema (
         |  (Node) FROM $nodesView,
         |  (Node)-[REL]->(Node)
         |    FROM $relsView edge
         |      START NODES (Node) FROM $nodesView alias_node JOIN ON alias_node.node_id = edge.source_id
         |      END NODES   (Node) FROM $nodesView alias_node JOIN ON alias_node.node_id = edge.target_id
         |)
     """.stripMargin

    sparkSession
      .createDataFrame(Seq(
        (0L, 23L, "startValue", "endValue"),
        (1L, 42L, "startValue", "endValue")
      )).repartition(1) // to keep id generation predictable
      .toDF("node_id", "id", "start", "end")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$nodesView")
    sparkSession
      .createDataFrame(Seq((0L, 1L, 1984L, "startValue", "endValue")))
      .toDF("source_id", "target_id", "id", "start", "end")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$relsView")

    val ds = SqlPropertyGraphDataSource(GraphDdl(ddlString), Map(dataSourceName -> Hive))

    val nodeId1 = computePartitionedRowId(rowIndex = 0, partitionStartDelta = 0)
    val nodeId2 = computePartitionedRowId(rowIndex = 1, partitionStartDelta = 0)

    ds.graph(fooGraphName).nodes("n").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n" -> CAPSNode(nodeId1, Set("Node"), CypherMap("id" -> 23, "start" -> "startValue", "end" -> "endValue"))),
      CypherMap("n" -> CAPSNode(nodeId2, Set("Node"), CypherMap("id" -> 42, "start" -> "startValue", "end" -> "endValue")))
    ))

    ds.graph(fooGraphName).relationships("r").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("r" -> CAPSRelationship(
        id = computePartitionedRowId(rowIndex = 0, partitionStartDelta = 0),
        startId = nodeId1,
        endId = nodeId2,
        relType = "REL",
        properties = CypherMap("id" -> 1984L, "start" -> "startValue", "end" -> "endValue")))
    ))
  }

  it("reads relationships from multiple tables") {
    val personView = "person_view"
    val bookView = "bookView_view"
    val readsView1 = "reads1_view"
    val readsView2 = "reads2_view"

    val ddlString =
      s"""
         |SET SCHEMA $dataSourceName.$databaseName
         |
         |CREATE GRAPH TYPE fooSchema (
         | Person ( name STRING ) ,
         | Book   ( title STRING ) ,
         | READS  ( rating FLOAT ) ,
         | (Person),
         | (Book),
         | (Person)-[READS]->(Book)
         |)
         |CREATE GRAPH fooGraph OF fooSchema (
         |  (Person) FROM $personView ( person_name AS name ),
         |  (Book) FROM $bookView (book_title AS title ),
         |  (Person)-[READS]->(Book)
         |    FROM $readsView1 edge
         |      START NODES (Person) FROM $personView alias_person JOIN ON alias_person.person_id = edge.person
         |      END NODES   (Book)   FROM $bookView   alias_book   JOIN ON edge.book = alias_book.book_id
         |    FROM $readsView2 edge (rates AS rating)
         |      START NODES (Person) FROM $personView alias_person JOIN ON edge.p_id = alias_person.person_id
         |      END NODES   (Book)   FROM $bookView   alias_book   JOIN ON alias_book.book_id = edge.b_id
         |)
     """.stripMargin

    sparkSession
      .createDataFrame(Seq((0L, "Alice")))
      .toDF("person_id", "person_name")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$personView")
    sparkSession
      .createDataFrame(Seq((1L, "1984"), (2L, "Scala with Cats"))).repartition(1) // to keep id generation predictable
      .toDF("book_id", "book_title")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$bookView")
    sparkSession
      .createDataFrame(Seq((0L, 1L, 42.23)))
      .toDF("person", "book", "rating")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$readsView1")
    sparkSession
      .createDataFrame(Seq((0L, 2L, 13.37)))
      .toDF("p_id", "b_id", "rates")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"$databaseName.$readsView2")

    val ds = SqlPropertyGraphDataSource(GraphDdl(ddlString), Map(dataSourceName -> Hive))

    val personId = computePartitionedRowId(rowIndex = 0, partitionStartDelta = 0)
    val book1Id = computePartitionedRowId(rowIndex = 0, partitionStartDelta = 1)
    val book2Id = computePartitionedRowId(rowIndex = 1, partitionStartDelta = 1)

    ds.graph(fooGraphName).nodes("n").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n" -> CAPSNode(personId, Set("Person"), CypherMap("name" -> "Alice"))),
      CypherMap("n" -> CAPSNode(book1Id, Set("Book"), CypherMap("title" -> "1984"))),
      CypherMap("n" -> CAPSNode(book2Id, Set("Book"), CypherMap("title" -> "Scala with Cats")))
    ))

    ds.graph(fooGraphName).relationships("r").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("r" -> CAPSRelationship(
        id = computePartitionedRowId(rowIndex = 0, partitionStartDelta = 0),
        startId = personId,
        endId = book1Id,
        relType = "READS",
        properties = CypherMap("rating" -> 42.23))),
      CypherMap("r" -> CAPSRelationship(
        id = computePartitionedRowId(rowIndex = 0, partitionStartDelta = 1),
        startId = personId,
        endId = book2Id,
        relType = "READS",
        properties = CypherMap("rating" -> 13.37)))

    ))
  }

  it("reads nodes from multiple data sources") {
    val fooView = "foo_view"
    val barView = "bar_view"

    val ddlString =
      s"""
         |CREATE GRAPH TYPE fooSchema (
         | Foo ( foo STRING ) ,
         | Bar ( bar INTEGER ) ,
         | (Foo),
         | (Bar)
         |)
         |CREATE GRAPH fooGraph OF fooSchema (
         |  (Foo) FROM ds1.db1.$fooView,
         |  (Bar) FROM ds2.db2.$barView
         |)
     """.stripMargin

    freshHiveDatabase("db1")
    freshHiveDatabase("db2")
    sparkSession
      .createDataFrame(Seq(Tuple1("Alice")))
      .toDF("foo")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"db1.$fooView")
    sparkSession
      .createDataFrame(Seq(Tuple1(0L)))
      .toDF("bar")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"db2.$barView")

    val configs = Map(
      "ds1" -> Hive,
      "ds2" -> Hive
    )
    val ds = SqlPropertyGraphDataSource(GraphDdl(ddlString), configs)

    ds.graph(fooGraphName).nodes("n").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n" -> CAPSNode(computePartitionedRowId(rowIndex = 0, partitionStartDelta = 0), Set("Foo"), CypherMap("foo" -> "Alice"))),
      CypherMap("n" -> CAPSNode(computePartitionedRowId(rowIndex = 0, partitionStartDelta = 1), Set("Bar"), CypherMap("bar" -> 0L)))
    ))
  }

  it("reads nodes from hive and h2 data sources") {
    val fooView = "foo_view"

    val ddlString =
      s"""
         |CREATE GRAPH TYPE fooSchema (
         | Foo ( foo STRING ) ,
         | Bar ( bar INTEGER ) ,
         | (Foo),
         | (Bar)
         |)
         |CREATE GRAPH fooGraph OF fooSchema (
         |  (Foo) FROM ds1.schema1.$fooView,
         |  (Bar) FROM ds2.schema2.barView
         |)
     """.stripMargin

    val hiveDataSourceConfig = Hive
    val h2DataSourceConfig = Jdbc(
      driver = "org.h2.Driver",
      url = "jdbc:h2:mem:?user=sa&password=1234;DB_CLOSE_DELAY=-1"
    )
    // -- Add hive data

    freshHiveDatabase("schema1")
    sparkSession
      .createDataFrame(Seq(Tuple1("Alice")))
      .toDF("foo")
      .write.mode(SaveMode.Overwrite).saveAsTable(s"schema1.$fooView")

    // -- Add h2 data
    import org.opencypher.spark.testing.utils.H2Utils._

    freshH2Database(h2DataSourceConfig, "schema2")
    sparkSession
      .createDataFrame(Seq(Tuple1(123L)))
      .toDF("bar")
      .saveAsSqlTable(h2DataSourceConfig, "schema2.barView")

    // -- Read graph and validate

    val ds = SqlPropertyGraphDataSource(GraphDdl(ddlString), Map("ds1" -> hiveDataSourceConfig, "ds2" -> h2DataSourceConfig))

    ds.graph(fooGraphName).nodes("n").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n" -> CAPSNode(computePartitionedRowId(rowIndex = 0, partitionStartDelta = 0), Set("Foo"), CypherMap("foo" -> "Alice"))),
      CypherMap("n" -> CAPSNode(computePartitionedRowId(rowIndex = 0, partitionStartDelta = 1), Set("Bar"), CypherMap("bar" -> 123L)))
    ))

  }

  it("should not auto-cast IntegerType columns to LongType") {
    val data = List(
      Row(1, 10L),
      Row(15, 800L)
    ).asJava
    val df = sparkSession.createDataFrame(data, StructType(Seq(StructField("int", IntegerType), StructField("long", LongType))))

    caps.sql("CREATE DATABASE IF NOT EXISTS db")
    df.write.saveAsTable("db.int_long")

    val ddlString =
      """
        |CREATE GRAPH TYPE fooType (
        | Foo (int INTEGER, long INTEGER),
        | (Foo)
        |)
        |
        |CREATE GRAPH fooGraph OF fooType (
        | (Foo) FROM ds1.db.int_long
        |)
        """.stripMargin

    val pgds = SqlPropertyGraphDataSource(GraphDdl(ddlString), Map("ds1" -> Hive))

    pgds.graph(GraphName("fooGraph")).cypher("MATCH (n) RETURN n.int, n.long").records.toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n.int" -> 1, "n.long" -> 10),
      CypherMap("n.int" -> 15, "n.long" -> 800)
    ))
  }

  it("should give good error message on bad SqlDataSource config") {
    val ddlString =
      """
        |CREATE GRAPH g (
        |  A,
        |  (A) FROM unknown.schema.a
        |)
      """.stripMargin

    val pgds = SqlPropertyGraphDataSource(GraphDdl(ddlString), Map("known1" -> Hive, "known2" -> Hive))

    val e = the [SqlDataSourceConfigException] thrownBy pgds.graph(GraphName("g"))
    e.getMessage should (include("unknown") and include("known1") and include("known2"))
  }

  it("reads nodes and rels from file-based sources") {
    val ddlString =
      s"""
         |CREATE GRAPH fooGraph (
         | Person (id INTEGER, name STRING),
         | KNOWS,
         |
         |  (Person) FROM csv.`Person.csv`,
         |
         |  (Person)-[KNOWS]->(Person)
         |    FROM csv.`KNOWS.csv` edge
         |      START NODES (Person) FROM csv.`Person.csv` person JOIN ON person.id = edge.p1
         |      END   NODES (Person) FROM csv.`Person.csv` person JOIN ON edge.p2 = person.id
         |)
     """.stripMargin

    // -- Read graph and validate
    val ds = SqlPropertyGraphDataSource(
      GraphDdl(ddlString),
      Map("csv" -> File(
        format = FileFormat.csv,
        basePath = Some("file://" + getClass.getResource("/csv").getPath)
      ))
    )

    ds.graph(fooGraphName).nodes("n").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n" -> CAPSNode(0, Set("Person"), CypherMap("id" -> 1, "name" -> "Alice"))),
      CypherMap("n" -> CAPSNode(1, Set("Person"), CypherMap("id" -> 2, "name" -> "Bob"))),
      CypherMap("n" -> CAPSNode(2, Set("Person"), CypherMap("id" -> 3, "name" -> "Eve")))
    ))

    ds.graph(fooGraphName).relationships("r").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("r" -> CAPSRelationship(0, 0, 1, "KNOWS", CypherMap.empty)),
      CypherMap("r" -> CAPSRelationship(1, 1, 2, "KNOWS", CypherMap.empty))
    ))
  }

  it("reads nodes and rels from file-based sources with absolute paths") {
    val basePath = "file://" + getClass.getResource("/csv").getPath
    val ddlString =
      s"""
         |CREATE GRAPH fooGraph (
         | Person (id INTEGER, name STRING),
         | KNOWS,
         |
         |  (Person) FROM csv.`$basePath/Person.csv`,
         |
         |  (Person)-[KNOWS]->(Person)
         |    FROM csv.`$basePath/KNOWS.csv` edge
         |      START NODES (Person) FROM csv.`$basePath/Person.csv` person JOIN ON person.id = edge.p1
         |      END   NODES (Person) FROM csv.`$basePath/Person.csv` person JOIN ON edge.p2 = person.id
         |)
     """.stripMargin

    // -- Read graph and validate
    val ds = SqlPropertyGraphDataSource(
      GraphDdl(ddlString),
      Map("csv" -> File(
        format = FileFormat.csv
      ))
    )

    ds.graph(fooGraphName).nodes("n").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("n" -> CAPSNode(0, Set("Person"), CypherMap("id" -> 1, "name" -> "Alice"))),
      CypherMap("n" -> CAPSNode(1, Set("Person"), CypherMap("id" -> 2, "name" -> "Bob"))),
      CypherMap("n" -> CAPSNode(2, Set("Person"), CypherMap("id" -> 3, "name" -> "Eve")))
    ))

    ds.graph(fooGraphName).relationships("r").toMapsWithCollectedEntities should equal(Bag(
      CypherMap("r" -> CAPSRelationship(0, 0, 1, "KNOWS", CypherMap.empty)),
      CypherMap("r" -> CAPSRelationship(1, 1, 2, "KNOWS", CypherMap.empty))
    ))
  }
}
