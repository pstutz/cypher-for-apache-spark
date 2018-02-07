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
package org.opencypher.caps.impl.record

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.encoders.{ExpressionEncoder, RowEncoder}
import org.apache.spark.sql.types.{StructField, StructType}
import org.opencypher.caps.api.exception.IllegalArgumentException
import org.opencypher.caps.impl.spark.SparkColumnName
import org.opencypher.caps.impl.spark.convert.SparkUtils._
import org.opencypher.caps.ir.api.expr.Var

object CAPSRecordHeader {

  def fromSparkStructType(structType: StructType): TableHeader =
    TableHeader.from(structType.fields.map { field =>
      OpaqueField(
        Var(field.name)(fromSparkType(field.dataType, field.nullable)
          .getOrElse(throw IllegalArgumentException("a supported Spark type", field.dataType))))
    }: _*)

  def asSparkStructType(header: TableHeader): StructType = {
    val fields = header.slots.map(slot => structField(slot, !header.mandatory(slot)))
    StructType(fields)
  }

  private def structField(slot: RecordSlot, nullable: Boolean): StructField = {
    val name = SparkColumnName.of(slot.content)
    val dataType = toSparkType(slot.content.cypherType)
    StructField(name, dataType, nullable)
  }

  implicit class CAPSRecordHeader(header: TableHeader) extends Serializable {
    def asSparkSchema: StructType =
      StructType(header.internalHeader.slots.map(_.asStructField))

    def rowEncoder: ExpressionEncoder[Row] =
      RowEncoder(asSparkSchema)
  }

  implicit class CAPSInternalHeader(internalHeader: InternalHeader) {
    def columns = internalHeader.slots.map(computeColumnName).toVector

    def column(slot: RecordSlot) = columns(slot.index)

    private def computeColumnName(slot: RecordSlot): String = SparkColumnName.of(slot)
  }

  implicit class CAPSRecordSlot(slot: RecordSlot) {
    def asStructField: StructField = {
      val name = SparkColumnName.of(slot)
      val sparkType = toSparkType(slot.content.cypherType)
      StructField(name, sparkType)
    }
  }
}
