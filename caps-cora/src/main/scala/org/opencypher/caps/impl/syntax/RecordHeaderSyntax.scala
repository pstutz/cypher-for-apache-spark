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
package org.opencypher.caps.impl.syntax

import cats.data.State
import cats.data.State.{get, set}
import org.opencypher.caps.impl.record._

import scala.language.implicitConversions

object RecordHeaderSyntax extends RecordHeaderSyntax

trait RecordHeaderSyntax {

  implicit def sparkRecordHeaderSyntax(header: TableHeader): RecordHeaderOps =
    new RecordHeaderOps(header)

  type HeaderState[X] = State[TableHeader, X]

  def addContents(contents: Seq[SlotContent]): State[TableHeader, Vector[AdditiveUpdateResult[RecordSlot]]] =
    exec(InternalHeader.addContents(contents))

  def addContent(content: SlotContent): State[TableHeader, AdditiveUpdateResult[RecordSlot]] =
    exec(InternalHeader.addContent(content))

  def compactFields(implicit details: RetainedDetails): State[TableHeader, Vector[RemovingUpdateResult[RecordSlot]]] =
    exec(InternalHeader.compactFields)

  private def exec[O](inner: State[InternalHeader, O]): State[TableHeader, O] =
    get[TableHeader]
      .map(header => inner.run(header.internalHeader).value)
      .flatMap { case (newInternalHeader, value) => set(TableHeader(newInternalHeader)).map(_ => value) }
}

final class RecordHeaderOps(header: TableHeader) {

  def +(content: SlotContent): TableHeader =
    header.copy(header.internalHeader + content)

  def update[A](ops: State[TableHeader, A]): (TableHeader, A) =
    ops.run(header).value
}
