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
package org.opencypher.caps.impl.spark.physical

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.types.{ArrayType, BooleanType, LongType}
import org.opencypher.caps.api.expr.{EndNode, Var}
import org.opencypher.caps.api.record.{OpaqueField, ProjectedExpr, RecordHeader, RecordSlot}
import org.opencypher.caps.api.spark.CAPSRecords
import org.opencypher.caps.api.types.CTNode
import org.opencypher.caps.impl.flat.FreshVariableNamer
import org.opencypher.caps.impl.spark.physical.PhysicalOperator.{assertIsNode, columnName, joinRecords}

sealed trait TernaryPhysicalOperator extends PhysicalOperator {
  override def execute(inputs: PhysicalResult*)(implicit context: RuntimeContext): PhysicalResult = {
    require(inputs.length == 3)
    executeTernary(inputs(0), inputs(1), inputs(2))
  }

  def executeTernary(first: PhysicalResult, second: PhysicalResult, third: PhysicalResult)(
      implicit context: RuntimeContext): PhysicalResult
}

// This maps a Cypher pattern such as (s)-[r]->(t), where s is solved by first, r is solved by second and t is solved by third
final case class ExpandSource(source: Var, rel: Var, target: Var, header: RecordHeader)
    extends TernaryPhysicalOperator {

  override def executeTernary(first: PhysicalResult, second: PhysicalResult, third: PhysicalResult)(
      implicit context: RuntimeContext) = {
    val sourceSlot = first.records.header.slotFor(source)
    val sourceSlotInRel = second.records.header.sourceNodeSlot(rel)
    assertIsNode(sourceSlot)
    assertIsNode(sourceSlotInRel)

    val sourceToRelHeader = first.records.header ++ second.records.header
    val sourceAndRel = joinRecords(sourceToRelHeader, Seq(sourceSlot -> sourceSlotInRel))(first.records, second.records)

    val targetSlot = third.records.header.slotFor(target)
    val targetSlotInRel = sourceAndRel.header.targetNodeSlot(rel)
    assertIsNode(targetSlot)
    assertIsNode(targetSlotInRel)

    val joinedRecords = joinRecords(header, Seq(targetSlotInRel -> targetSlot))(sourceAndRel, third.records)
    PhysicalResult(joinedRecords, first.graphs ++ second.graphs ++ third.graphs)
  }
}

// Expands a pattern like (s)-[r*n..m]->(t) where s is solved by first, r is solved by second and t is solved by third
// this performs m joins with second to step all steps, then drops n of these steps
// edgeList is what is bound to r; a list of relationships (currently just the ids)
final case class BoundedVarExpand(
    rel: Var,
    edgeList: Var,
    target: Var,
    initialEndNode: Var,
    lower: Int,
    upper: Int,
    header: RecordHeader,
    isExpandInto: Boolean)
    extends TernaryPhysicalOperator {

  override def executeTernary(first: PhysicalResult, second: PhysicalResult, third: PhysicalResult)(
      implicit context: RuntimeContext): PhysicalResult = {
    val expanded = expand(first.records, second.records)

    PhysicalResult(finalize(expanded, third.records), first.graphs ++ second.graphs ++ third.graphs)
  }

  private def iterate(lhs: DataFrame, rels: DataFrame)(
      endNode: RecordSlot,
      rel: Var,
      relStartNode: RecordSlot,
      listTempColName: String,
      edgeListColName: String,
      keep: Array[String]): DataFrame = {

    val relIdColumn = rels.col(columnName(OpaqueField(rel)))
    val startColumn = rels.col(columnName(relStartNode))
    val expandColumnName = columnName(endNode)
    val expandColumn = lhs.col(expandColumnName)

    val joined = lhs.join(rels, expandColumn === startColumn, "inner")

    val appendUdf = udf(udfUtils.arrayAppend _, ArrayType(LongType))
    val extendedArray = appendUdf(lhs.col(edgeListColName), relIdColumn)
    val withExtendedArray = joined.withColumn(listTempColName, extendedArray)
    val arrayContains = udf(udfUtils.contains _, BooleanType)(withExtendedArray.col(edgeListColName), relIdColumn)
    val filtered = withExtendedArray.filter(!arrayContains)

    // TODO: Try and get rid of the Var rel here
    val endNodeIdColNameOfJoinedRel = columnName(ProjectedExpr(EndNode(rel)(CTNode)))

    val columns = keep ++ Seq(listTempColName, endNodeIdColNameOfJoinedRel)
    val withoutRelProperties = filtered.select(columns.head, columns.tail: _*) // drops joined columns from relationship table

    withoutRelProperties
      .drop(expandColumn)
      .withColumnRenamed(endNodeIdColNameOfJoinedRel, expandColumnName)
      .drop(edgeListColName)
      .withColumnRenamed(listTempColName, edgeListColName)
  }

  private def finalize(expanded: CAPSRecords, targets: CAPSRecords): CAPSRecords = {
    val endNodeSlot = expanded.header.slotFor(initialEndNode)
    val endNodeCol = columnName(endNodeSlot)

    val targetNodeSlot = targets.header.slotFor(target)
    val targetNodeCol = columnName(targetNodeSlot)

    // If the expansion ends in an already solved plan, the final join can be replaced by a filter.
    val result = if (isExpandInto) {
      val data = expanded.toDF()
      CAPSRecords.create(header, data.filter(data.col(targetNodeCol) === data.col(endNodeCol)))(expanded.caps)
    } else {
      val joinHeader = expanded.header ++ targets.header

      val lhsSlot = expanded.header.slotFor(initialEndNode)
      val rhsSlot = targets.header.slotFor(target)

      assertIsNode(lhsSlot)
      assertIsNode(rhsSlot)

      joinRecords(joinHeader, Seq(lhsSlot -> rhsSlot))(expanded, targets)
    }

    CAPSRecords.create(header, result.toDF().drop(endNodeCol))(expanded.caps)
  }

  private def expand(firstRecords: CAPSRecords, secondRecords: CAPSRecords): CAPSRecords = {
    val initData = firstRecords.data
    val relsData = secondRecords.data

    val edgeListColName = columnName(firstRecords.header.slotFor(edgeList))

    val steps = new collection.mutable.HashMap[Int, DataFrame]
    steps(0) = initData

    val keep = initData.columns

    val listTempColName =
      FreshVariableNamer.generateUniqueName(firstRecords.header)

    val startSlot = secondRecords.header.sourceNodeSlot(rel)
    val endNodeSlot = firstRecords.header.slotFor(initialEndNode)
    (1 to upper).foreach { i =>
      // TODO: Check whether we can abort iteration if result has no cardinality (eg count > 0?)
      steps(i) = iterate(steps(i - 1), relsData)(endNodeSlot, rel, startSlot, listTempColName, edgeListColName, keep)
    }

    val union = steps.filterKeys(_ >= lower).values.reduce[DataFrame] {
      case (l, r) => l.union(r)
    }

    CAPSRecords.create(firstRecords.header, union)(firstRecords.caps)
  }

}
