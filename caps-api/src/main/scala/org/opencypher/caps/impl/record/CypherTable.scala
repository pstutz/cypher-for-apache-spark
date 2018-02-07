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

import org.opencypher.caps.api.types.CypherType
import org.opencypher.caps.api.value.CypherValue._

trait CypherRow {
  def apply[T <: CypherValue](columnName: String): CypherValue

  def getColumn[T <: CypherValue](columnName: String): Option[CypherValue]
}

/**
  * Represents a table in which each row contains Cypher values.
  * Each row contains an evaluated Cypher expression per column.
  */
trait CypherTable extends CypherPrintable {

  lazy val columnNames: Set[String] = columns.toSet

  def columnType(columnName: String): CypherType

  def columns: Seq[String]

  /**
    * Iterate over the rows, each row is converted to a CypherMap from column name to the value stored in that column.
    *
    * WARNING: This operation may be very expensive as it may have to materialise the results.
    */
  def iterator: Iterator[CypherMap] = {
    rows.map { row: CypherRow =>
      val mapEntries = columns.map(columnName => columnName -> row(columnName))
      CypherMap(mapEntries: _*)
    }
  }

  /**
    * Iterate over the rows in this Cypher table.
    *
    * WARNING: This operation may be very expensive as it may have to materialise the results.
    */
  def rows: Iterator[CypherRow]

  /**
    * @return number of rows in this CypherTable.
    */
  def size: Long
}
