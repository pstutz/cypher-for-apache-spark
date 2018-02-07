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

import org.opencypher.caps.api.exception.IllegalArgumentException
import org.opencypher.caps.api.schema.Schema
import org.opencypher.caps.api.types.{CTBoolean, CTNode, CTString, _}
import org.opencypher.caps.impl.syntax.RecordHeaderSyntax._
import org.opencypher.caps.ir.api.expr._
import org.opencypher.caps.ir.api.{Label, PropertyKey}

/**
  * A header for a CypherRecords.
  *
  * The header consists of a number of slots, each of which represents a Cypher expression.
  * The slots that represent variables (which is a kind of expression) are called <i>fields</i>.
  */
final case class TableHeader(internalHeader: InternalHeader) extends CypherTableHeader {

  /**
    * Computes the concatenation of this header and another header.
    *
    * @param other the header with which to concatenate.
    * @return the concatenation of this and the argument header.
    */
  def ++(other: TableHeader): TableHeader =
    copy(internalHeader ++ other.internalHeader)

  def indexOf(content: SlotContent): Option[Int] = slots.find(_.content == content).map(_.index)

  /**
    * The ordered sequence of slots stored in this header.
    *
    * @return the slots in this header.
    */
  def slots: IndexedSeq[RecordSlot] = internalHeader.slots
  def contents: Set[SlotContent] = slots.map(_.content).toSet

  /**
    * The set of fields contained in this header.
    *
    * @return the fields in this header.
    */
  def fields: Set[String] = internalHeader.fields.map(_.name)

  /**
    * The fields contained in this header, in the order they were defined.
    *
    * @return the ordered fields in this header.
    */
  def fieldsInOrder: Seq[String] = slots.flatMap(_.content.alias.map(_.name))

  def slotsFor(expr: Expr): Seq[RecordSlot] =
    internalHeader.slotsFor(expr)

  // TODO: Push error handling to API consumers

  def slotFor(variable: Var): RecordSlot =
    slotsFor(variable).headOption.getOrElse(???)

  def mandatory(slot: RecordSlot): Boolean =
    internalHeader.mandatory(slot)

  def sourceNodeSlot(rel: Var): RecordSlot = slotsFor(StartNode(rel)()).headOption.getOrElse(???)
  def targetNodeSlot(rel: Var): RecordSlot = slotsFor(EndNode(rel)()).headOption.getOrElse(???)
  def typeSlot(rel: Expr): RecordSlot = slotsFor(Type(rel)()).headOption.getOrElse(???)

  def labels(node: Var): Seq[HasLabel] = labelSlots(node).keys.toSeq

  def properties(node: Var): Seq[Property] = propertySlots(node).keys.toSeq

  def select(fields: Set[Var]): TableHeader = {
    fields.foldLeft(TableHeader.empty) {
      case (acc, next) =>
        val contents = childSlots(next).map(_.content)
        if (contents.nonEmpty) {
          acc.update(addContents(OpaqueField(next) +: contents))._1
        } else {
          acc
        }
    }
  }

  def selfWithChildren(field: Var): Seq[RecordSlot] =
    slotFor(field) +: childSlots(field)

  def childSlots(entity: Var): Seq[RecordSlot] = {
    slots.filter {
      case RecordSlot(_, OpaqueField(_))               => false
      case slot if slot.content.owner.contains(entity) => true
      case _                                           => false
    }
  }

  def labelSlots(node: Var): Map[HasLabel, RecordSlot] = {
    slots.collect {
      case s @ RecordSlot(_, ProjectedExpr(h: HasLabel)) if h.node == node     => h -> s
      case s @ RecordSlot(_, ProjectedField(_, h: HasLabel)) if h.node == node => h -> s
    }.toMap
  }

  def propertySlots(entity: Var): Map[Property, RecordSlot] = {
    slots.collect {
      case s @ RecordSlot(_, ProjectedExpr(p: Property)) if p.m == entity     => p -> s
      case s @ RecordSlot(_, ProjectedField(_, p: Property)) if p.m == entity => p -> s
    }.toMap
  }

  def nodesForType(nodeType: CTNode): Seq[Var] = {
    slots.collect {
      case RecordSlot(_, OpaqueField(v)) => v
    }.filter { v =>
      v.cypherType match {
        case CTNode(labels) =>
          val allPossibleLabels = this.labels(v).map(_.label.name).toSet ++ labels
          nodeType.labels.subsetOf(allPossibleLabels)
        case _ => false
      }
    }
  }

  def relationshipsForType(relType: CTRelationship): Seq[Var] = {
    val targetTypes = relType.types

    slots.collect {
      case RecordSlot(_, OpaqueField(v)) => v
    }.filter { v =>
      v.cypherType match {
        case t: CTRelationship if targetTypes.isEmpty || t.types.isEmpty => true
        case CTRelationship(types) =>
          types.exists(targetTypes.contains)
        case _ => false
      }
    }
  }

  override def toString: String = s"RecordHeader with ${slots.size} slots"

  def pretty: String = s"RecordHeader with ${slots.size} slots: \n\t ${slots.mkString("\n\t")}"
}

object TableHeader {

  def empty: TableHeader =
    TableHeader(InternalHeader.empty)

  def from(contents: SlotContent*): TableHeader =
    TableHeader(contents.foldLeft(InternalHeader.empty) { case (header, slot) => header + slot })

  // TODO: Probably move this to an implicit class RichSchema?
  def nodeFromSchema(node: Var, schema: Schema): TableHeader = {
    val labels: Set[String] = node.cypherType match {
      case CTNode(l) => l
      case other     => throw IllegalArgumentException("CTNode", other)
    }
    nodeFromSchema(node, schema, labels)
  }

  def nodeFromSchema(node: Var, schema: Schema, labels: Set[String]): TableHeader = {

    val labelCombos = if (labels.isEmpty) {
      // all nodes scan
      schema.allLabelCombinations
    } else {
      // label scan
      val impliedLabels = schema.impliedLabels.transitiveImplicationsFor(labels)
      schema.combinationsFor(impliedLabels)
    }

    // create a label column for each possible label
    // optimisation enabled: will not add columns for implied or impossible labels
    val labelExprs = labelCombos.flatten.toSeq.sorted.map { label =>
      ProjectedExpr(HasLabel(node, Label(label))(CTBoolean))
    }

    val propertyKeys = schema.keysFor(labelCombos)
    val propertyExprs = propertyKeys.toSeq.sortBy(_._1).map {
      case (k, t) => ProjectedExpr(Property(node, PropertyKey(k))(t))
    }

    val projectedExprs = labelExprs ++ propertyExprs
    val (header, _) = TableHeader.empty
      .update(addContents(OpaqueField(node) +: projectedExprs))

    header
  }

  def relationshipFromSchema(rel: Var, schema: Schema): TableHeader = {
    val types: Set[String] = rel.cypherType match {
      case CTRelationship(_types) if _types.isEmpty =>
        schema.relationshipTypes
      case CTRelationship(_types) =>
        _types
      case other =>
        throw IllegalArgumentException("CTRelationship", other)
    }

    relationshipFromSchema(rel, schema, types)
  }

  def relationshipFromSchema(rel: Var, schema: Schema, relTypes: Set[String]): TableHeader = {
    val relKeyHeaderProperties = relTypes.toSeq
      .flatMap(t => schema.relationshipKeys(t).toSeq)
      .groupBy(_._1)
      .mapValues { keys =>
        if (keys.size == relTypes.size && keys.forall(keys.head == _)) {
          keys.head._2
        } else {
          keys.head._2.nullable
        }
      }

    val relKeyHeaderContents = relKeyHeaderProperties.toSeq.sortBy(_._1).map {
      case ((k, t)) => ProjectedExpr(Property(rel, PropertyKey(k))(t))
    }

    val startNode = ProjectedExpr(StartNode(rel)(CTNode))
    val typeString = ProjectedExpr(Type(rel)(CTString))
    val endNode = ProjectedExpr(EndNode(rel)(CTNode))

    val relHeaderContents = Seq(startNode, OpaqueField(rel), typeString, endNode) ++ relKeyHeaderContents
    val (relHeader, _) = TableHeader.empty.update(addContents(relHeaderContents))

    relHeader
  }
}
