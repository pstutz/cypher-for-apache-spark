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
package org.opencypher.caps.api.value

import org.opencypher.caps.api.types._

trait CypherValue {
  def cypherType: CypherType
}

trait CypherEntity extends CypherValue {
  override def cypherType: CTEntity

  def properties: CypherMap
}

trait CypherNode extends CypherEntity {
  override def cypherType: CTNode

  def labels: Set[String]
}

trait CypherRelationship extends CypherEntity {
  override def cypherType: CTRelationship

  def relationshipType: String
}

trait CypherList extends CypherValue {
  override def cypherType: CTList

  // TODO: Type-safe access to elements with more specialized types
  def values: Seq[CypherValue]
}

trait CypherBoolean extends CypherValue {
  override def cypherType = CTBoolean
}

trait CypherInteger extends CypherValue {
  override def cypherType = CTInteger

  def value: Long
}

trait CypherFloat extends CypherValue {
  override def cypherType = CTFloat

  def value: Double
}

trait CypherString extends CypherValue {
  override def cypherType = CTString

  def value: String
}

trait CypherMap extends CypherValue {
  override def cypherType = CTMap

  def get(key: String): Option[CypherValue]

  def keys: Set[String]
}

trait CypherPath extends CypherValue {
  override def cypherType = CTPath

  def startingNode: CypherNode

  def connections: List[CypherPath.Connection]
}

object CypherPath {

  sealed trait Connection {
    def node: CypherNode

    def relationship: CypherRelationship
  }

  trait Forward extends Connection

  trait Backward extends Connection

}
