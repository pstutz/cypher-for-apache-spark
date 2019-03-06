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
package org.opencypher.okapi.impl.configuration

import scala.util.Try

abstract class ConfigOption[T](val name: String, val defaultValue: T)(val convert: String => Option[T]) {

  def set(v: String): Unit = {
    System.setProperty(name, v)
    ()
  }

  def get: T = Option(System.getProperty(name)).flatMap(convert).getOrElse(defaultValue)

  override def toString: String = {
    val padded = name.padTo(25, " ").mkString("")
    s"$padded = $get"
  }
}

abstract class ConfigFlag(name: String, defaultValue: Boolean = false)
  extends ConfigOption[Boolean](name, defaultValue)(s => Try(s.toBoolean).toOption) {

  def set(): Unit = set(true.toString)

  def isSet: Boolean = get
}

trait ConfigCaching[T] {

  self: ConfigOption[T] =>

  override lazy val get: T = Option(System.getProperty(name)).flatMap(convert).getOrElse(defaultValue)

  override def set(v: String): Unit = {
    System.setProperty(name, v)
    if (get != convert(v).get) {
      throw new UnsupportedOperationException(
        "A cached config cannot be set to a different value after it was read"
      )
    }
  }

}
