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
package org.opencypher.spark.testing.support.creation.caps

import java.time.{LocalDate, LocalDateTime}

import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{LongType, StructField, StructType}
import org.opencypher.okapi.api.graph._
import org.opencypher.okapi.api.schema.PropertyKeys.PropertyKeys
import org.opencypher.okapi.api.schema.Schema
import org.opencypher.okapi.api.types.{CTNode, CTRelationship}
import org.opencypher.okapi.api.value.CypherValue.{CypherEntity, CypherValue}
import org.opencypher.okapi.impl.exception.{IllegalArgumentException, IllegalStateException}
import org.opencypher.okapi.impl.temporal.Duration
import org.opencypher.okapi.relational.impl.graph.ScanGraph
import org.opencypher.okapi.testing.propertygraph.{InMemoryTestGraph, InMemoryTestNode, InMemoryTestRelationship}
import org.opencypher.spark.api.CAPSSession
import org.opencypher.spark.api.io.CAPSEntityTable
import org.opencypher.spark.impl.convert.SparkConversions._
import org.opencypher.spark.impl.table.SparkTable.DataFrameTable
import org.opencypher.spark.impl.temporal.SparkTemporalHelpers._
import org.opencypher.spark.schema.CAPSSchema._
import org.opencypher.spark.testing.support.EntityTableCreationSupport

import scala.collection.JavaConverters._

object CAPSScanGraphFactory extends CAPSTestGraphFactory with EntityTableCreationSupport {

  override def apply(propertyGraph: InMemoryTestGraph, additionalPatterns: Seq[Pattern])
    (implicit caps: CAPSSession): ScanGraph[DataFrameTable] = {

    val schema = computeSchema(propertyGraph).asCaps

    val nodePatterns = schema.labelCombinations.combos.map(labels => NodePattern(CTNode(labels)))
    val relPatterns = schema.relationshipTypes.map(typ => RelationshipPattern(CTRelationship(typ)))

    val scans = (nodePatterns ++ relPatterns ++ additionalPatterns).map { pattern =>
      val data = extractEmbeddings(pattern, propertyGraph, schema)
      createEntityTable(pattern, data, schema)
    }

    new ScanGraph(scans.toSeq, schema)
  }

  override def name: String = "CAPSScanGraphFactory"

  private def extractEmbeddings(pattern: Pattern, graph: InMemoryTestGraph, schema: Schema)
    (implicit caps: CAPSSession): Seq[Map[Entity, CypherEntity[Long]]] = {

    val candidates = pattern.entities.map { entity =>
      entity.cypherType match {
        case CTNode(labels, _) =>
          entity -> graph.nodes.filter(_.labels == labels)
        case CTRelationship(types, _) =>
          entity -> graph.relationships.filter(rel => types.contains(rel.relType))
        case other => throw IllegalArgumentException("Node or Relationship type", other)
      }
    }.toMap

    val unitEmbedding = Seq(
      Map.empty[Entity, CypherEntity[Long]]
    )
    val initialEmbeddings = pattern.entities.foldLeft(unitEmbedding) {
      case (acc, entity) =>
        val entityCandidates = candidates(entity)

        for {
          row <- acc
          entityCandidate <- entityCandidates
        } yield row.updated(entity, entityCandidate)
    }

    pattern.topology.foldLeft(initialEmbeddings) {
      case (acc, (relEntity, connection)) =>
        connection match {
          case Connection(Some(sourceNode), None, _) => acc.filter { row =>
            row(sourceNode).id == row(relEntity).asInstanceOf[InMemoryTestRelationship].startId
          }

          case Connection(None, Some(targetEntity), _) => acc.filter { row =>
            row(targetEntity).id == row(relEntity).asInstanceOf[InMemoryTestRelationship].endId
          }

          case Connection(Some(sourceNode), Some(targetEntity), _) => acc.filter { row =>
            val rel = row(relEntity).asInstanceOf[InMemoryTestRelationship]
            row(sourceNode).id == rel.startId && row(targetEntity).id == rel.endId
          }

          case Connection(None, None, _) => throw IllegalStateException("Connection without source or target node")
        }
    }
  }

  private def createEntityTable(
    pattern: Pattern,
    embeddings: Seq[Map[Entity, CypherEntity[Long]]],
    schema: Schema
  )(implicit caps: CAPSSession): CAPSEntityTable = {

    val unitData: Seq[Seq[Any]] = Seq(embeddings.indices.map(_ => Seq.empty[Any]): _*)

    val (columns, data) = pattern.entities.foldLeft(Seq.empty[StructField] -> unitData) {
      case ((accColumns, accData), entity) =>

        entity.cypherType match {
          case CTNode(labels, _) =>
            val propertyKeys = schema.nodePropertyKeys(labels)
            val propertyFields = getPropertyStructFields(entity, propertyKeys)

            val nodeData = embeddings.map { embedding =>
              val node = embedding(entity).asInstanceOf[InMemoryTestNode]

              val propertyValues = propertyKeys.keySet.toSeq.map(p => node.properties.get(p).map(toSparkValue).orNull)
              Seq(node.id) ++ propertyValues
            }

            val newData = accData.zip(nodeData).map { case (l, r) => l ++ r }
            val newColumns = accColumns ++ Seq(StructField(s"${entity.name}_id", LongType)) ++ propertyFields

            newColumns -> newData


          case CTRelationship(types, _) =>
            val propertyKeys = schema.relationshipPropertyKeys(types.head)
            val propertyFields = getPropertyStructFields(entity, propertyKeys)

            val relData = embeddings.map { embedding =>
              val rel = embedding(entity).asInstanceOf[InMemoryTestRelationship]
              val propertyValues = propertyKeys.keySet.toSeq.map(p => rel.properties.get(p).map(toSparkValue).orNull)
              Seq(rel.id, rel.startId, rel.endId) ++ propertyValues
            }

            val newData = accData.zip(relData).map { case (l, r) => l ++ r }
            val newColumns = accColumns ++
              Seq(
                StructField(s"${entity.name}_id", LongType),
                StructField(s"${entity.name}_source", LongType),
                StructField(s"${entity.name}_target", LongType)
              ) ++
              propertyFields

            newColumns -> newData

          case other => throw IllegalArgumentException("Node or Relationship type", other)
        }
    }

    val df = caps.sparkSession.createDataFrame(
      data.map { r => Row(r: _*) }.asJava,
      StructType(columns)
    )

    constructEntityTable(pattern, df)
  }

  protected def getPropertyStructFields(entity: Entity, propKeys: PropertyKeys): Seq[StructField] = {
    propKeys.foldLeft(Seq.empty[StructField]) {
      case (fields, key) => fields :+ StructField(s"${entity.name}_${key._1}_property", key._2.getSparkType, key._2.isNullable)
    }
  }

  private def toSparkValue(v: CypherValue): Any = {
    v.getValue match {
      case Some(date: LocalDate) => java.sql.Date.valueOf(date)
      case Some(localDateTime: LocalDateTime) => java.sql.Timestamp.valueOf(localDateTime)
      case Some(dur: Duration) => dur.toCalendarInterval
      case Some(l: List[_]) => l.collect { case c: CypherValue => toSparkValue(c) }
      case Some(other) => other
      case None => null
    }
  }
}
