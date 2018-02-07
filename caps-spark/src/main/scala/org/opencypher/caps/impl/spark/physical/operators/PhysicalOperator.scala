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
package org.opencypher.caps.impl.spark.physical.operators

import java.net.URI

import org.apache.spark.sql.{Column, DataFrame, functions}
import org.opencypher.caps.api.CAPSSession
import org.opencypher.caps.api.exception.IllegalArgumentException
import org.opencypher.caps.api.types._
import org.opencypher.caps.impl.record.{TableHeader, RecordSlot, SlotContent}
import org.opencypher.caps.impl.spark.CAPSConverters._
import org.opencypher.caps.impl.spark.physical.{PhysicalResult, RuntimeContext}
import org.opencypher.caps.impl.spark.{CAPSGraph, CAPSRecords, SparkColumnName}
import org.opencypher.caps.trees.AbstractTreeNode

private[caps] abstract class PhysicalOperator extends AbstractTreeNode[PhysicalOperator] {

  def header: TableHeader

  def execute(implicit context: RuntimeContext): PhysicalResult

  protected def resolve(uri: URI)(implicit context: RuntimeContext): CAPSGraph = {
    context.resolve(uri).map(_.asCaps).getOrElse(throw IllegalArgumentException(s"a graph at $uri"))
  }

  override def args: Iterator[Any] = super.args.flatMap {
    case TableHeader(_) | Some(TableHeader(_)) => None
    case other                                   => Some(other)
  }
}

object PhysicalOperator {
  def columnName(slot: RecordSlot): String = SparkColumnName.of(slot)

  def columnName(content: SlotContent): String = SparkColumnName.of(content)

  def joinRecords(
      header: TableHeader,
      joinSlots: Seq[(RecordSlot, RecordSlot)],
      joinType: String = "inner",
      deduplicate: Boolean = false)(lhs: CAPSRecords, rhs: CAPSRecords): CAPSRecords = {

    val lhsData = lhs.toDF()
    val rhsData = rhs.toDF()

    val joinCols = joinSlots.map(pair => lhsData.col(columnName(pair._1)) -> rhsData.col(columnName(pair._2)))

    joinDFs(lhsData, rhsData, header, joinCols)(joinType, deduplicate)(lhs.caps)
  }

  def joinDFs(lhsData: DataFrame, rhsData: DataFrame, header: TableHeader, joinCols: Seq[(Column, Column)])(
      joinType: String,
      deduplicate: Boolean)(implicit caps: CAPSSession): CAPSRecords = {

    val joinExpr = joinCols.map { case (l, r) => l === r }
      .foldLeft(functions.lit(true))(_ && _)

    val joinedData = lhsData.join(rhsData, joinExpr, joinType)

    val returnData = if (deduplicate) {
      val colsToDrop = joinCols.map(col => col._2)
      colsToDrop.foldLeft(joinedData)((acc, col) => acc.drop(col))
    } else joinedData

    CAPSRecords.verifyAndCreate(header, returnData)
  }

  def assertIsNode(slot: RecordSlot): Unit = {
    slot.content.cypherType match {
      case CTNode(_) =>
      case x =>
        throw IllegalArgumentException(s"Expected $slot to contain a node, but was $x")
    }
  }

}
