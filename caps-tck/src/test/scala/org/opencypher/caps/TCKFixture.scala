/*
 * Copyright (c) 2016-2017 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.caps

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.function.Executable
import org.opencypher.caps.api.spark.{CAPSGraph, CAPSRecords, CAPSSession}
import org.opencypher.caps.api.value.{CypherValue => CAPSValue}
import org.opencypher.caps.impl.exception.Raise
import org.opencypher.caps.test.support.creation.caps.CAPSScanGraphFactory
import org.opencypher.caps.test.support.creation.propertygraph.{Neo4jPropertyGraphFactory, PropertyGraph}
import org.opencypher.tools.tck.api._
import org.opencypher.tools.tck.values.CypherValue

import scala.io.Source

// this is an object with a val because we can only load the
// scenarios _once_ due to a bug in the TCK API
object TCKFixture {
  val scenarios: Seq[Scenario] = CypherTCK.allTckScenarios

  def dynamicTest(graph: Graph)(scenario: Scenario): DynamicTest =
    DynamicTest.dynamicTest(scenario.toString, new Executable {
      override def execute(): Unit = {
        println(scenario)
        scenario(graph).execute()
      }
    })

  implicit val caps: CAPSSession = CAPSSession.local()
}

case class Neo4jBackedTestGraph(implicit caps: CAPSSession) extends Graph {
  private val neo4jGraph = new Neo4jPropertyGraphFactory

  implicit val converter: PropertyGraph => CAPSGraph = CAPSScanGraphFactory(_).graph

  override def execute(query: String, params: Map[String, CypherValue], queryType: QueryType): (Graph, Result) = {
    queryType match {
      case InitQuery =>
        val propertyGraph = neo4jGraph.create(query, Map.empty)
        val capsGraph = CAPSScanGraphFactory(propertyGraph).graph

        AsTckGraph(capsGraph) -> CypherValueRecords.empty
      case _ =>
        ???
    }
  }
}

case class AsTckGraph(graph: CAPSGraph) extends Graph {
  override def execute(query: String, params: Map[String, CypherValue], queryType: QueryType): (Graph, Result) = {
    queryType match {
      case InitQuery =>
        // we don't support updates on this adapter
        Raise.notYetImplemented("update queries for CAPS graphs")
      case SideEffectQuery =>
        // this one is tricky, not sure how can do it without Cypher
        this -> CypherValueRecords.empty
      case ExecQuery =>
        // mapValues is lazy, so we force it for debug purposes
        val capsResult = graph.cypher(query, params.mapValues(CAPSValue(_)).view.force)
        val tckRecords = convertToTckStrings(capsResult.records)

        this -> tckRecords
    }
  }

  private def convertToTckStrings(records: CAPSRecords): StringRecords = {
    val header = records.header.fieldsInOrder.map(_.name).toList
    val rows = records.toLocalScalaIterator.map { cypherMap =>
      cypherMap.keys.map(k => k -> java.util.Objects.toString(cypherMap.get(k).get)).toMap
    }.toList

    StringRecords(header, rows)
  }
}

object ScenarioBlacklist {
  private lazy val blacklist: Set[String] = {
    val blacklistIter = Source.fromFile(getClass.getResource("scenario_blacklist").toURI).getLines().toSeq
    val blacklistSet = blacklistIter.toSet

    lazy val errorMessage = s"Blacklist contains duplicate scenarios ${blacklistIter.groupBy(identity).filter(_._2.lengthCompare(1) > 0).keys.mkString("\n")}"
    assert(blacklistIter.lengthCompare(blacklistSet.size) == 0, errorMessage)
    blacklistSet
  }

  def contains(name: String): Boolean = blacklist.contains(name)
}
