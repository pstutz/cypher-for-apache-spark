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
package org.opencypher.okapi.api.schema

import cats.instances.all._
import cats.syntax.semigroup._
import org.opencypher.okapi.ApiBaseTest
import org.opencypher.okapi.api.schema.LabelPropertyMap._
import org.opencypher.okapi.api.types.CypherType.joinMonoid
import org.opencypher.okapi.api.types.{CTAny, CTBoolean, CTInteger, CTString}

class LabelPropertyMapTest extends ApiBaseTest {

  it("|+|") {
    val map1 = LabelPropertyMap.empty
      .register("A")("name" -> CTString, "age" -> CTInteger, "gender" -> CTString)
      .register("B")("p" -> CTBoolean)

    val map2 = LabelPropertyMap.empty
      .register("A")("name" -> CTString, "gender" -> CTBoolean)
      .register("C")("name" -> CTString)

    map1 |+| map2 should equal(
      LabelPropertyMap.empty
        .register("A")("name" -> CTString, "age" -> CTInteger, "gender" -> CTAny)
        .register("B")("p" -> CTBoolean)
        .register("C")("name" -> CTString)
    )
  }

  it("for labels") {
    val map = LabelPropertyMap.empty
      .register("A")("name" -> CTString)
      .register("B")("foo" -> CTInteger)
      .register("B", "A")("foo" -> CTInteger)
      .register("C")("bar" -> CTInteger)

    map.filterForLabels("A") should equal(LabelPropertyMap.empty
      .register("A")("name" -> CTString)
      .register("A", "B")("foo" -> CTInteger)
    )
    map.filterForLabels("C") should equal(LabelPropertyMap.empty
      .register("C")("bar" -> CTInteger)
    )
    map.filterForLabels("X") should equal(LabelPropertyMap.empty)
    map.filterForLabels("A", "B", "C") should equal(map)
  }

}
