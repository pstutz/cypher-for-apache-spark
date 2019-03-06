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
package org.opencypher.okapi.api.types

import cats.Monoid
import cats.instances.all._
import cats.syntax.semigroup._
import org.opencypher.okapi.api.graph.QualifiedGraphName
import org.opencypher.okapi.api.types.CypherType._
import org.opencypher.okapi.api.value.CypherValue._
import org.opencypher.okapi.impl.types.CypherTypeParser
import upickle.default._

object CypherType {

  implicit def rw: ReadWriter[CypherType] = readwriter[String].bimap[CypherType](_.name, s => fromName(s).get)

  /**
    * Parses the name of CypherType into the actual CypherType object.
    *
    * @param name string representation of the CypherType
    * @return
    * @see {{{org.opencypher.okapi.api.types.CypherType#name}}}
    */
  def fromName(name: String): Option[CypherType] = {
   CypherTypeParser.parseCypherType(name)
  }

  implicit class TypeCypherValue(cv: CypherValue) {
    def cypherType: CypherType = {
      cv match {
        case CypherNull => CTNull
        case CypherBoolean(_) => CTBoolean
        case CypherFloat(_) => CTFloat
        case CypherInteger(_) => CTInteger
        case CypherString(_) => CTString
        case CypherLocalDateTime(_) => CTLocalDateTime
        case CypherDate(_) => CTDate
        case CypherDuration(_) => CTDuration
        case CypherMap(inner) => CTMap(inner.mapValues(_.cypherType))
        case CypherNode(_, labels, _) => CTNode(labels)
        case CypherRelationship(_, _, _, relType, _) => CTRelationship(relType)
        case CypherList(l) => CTList(l.map(_.cypherType).foldLeft[CypherType](CTVoid)(_.join(_)))
      }
    }
  }

  implicit val joinMonoid: Monoid[CypherType] = new Monoid[CypherType] {
    override def empty: CypherType = CTVoid

    override def combine(x: CypherType, y: CypherType): CypherType = x join y
  }

}

case object CTAny extends MaterialDefiniteCypherType with MaterialDefiniteCypherType.DefaultOrNull {
  override def name = "ANY"

  override def superTypeOf(other: CypherType): Boolean = !other.isNullable

  override def joinMaterially(other: MaterialCypherType): MaterialCypherType = this

  override def meetMaterially(other: MaterialCypherType): MaterialCypherType = other
}

case object CTBoolean extends MaterialDefiniteCypherLeafType {
  override def name = "BOOLEAN"
}

case object CTNumber extends MaterialDefiniteCypherType with MaterialDefiniteCypherType.DefaultOrNull {

  self =>

  override def name = "NUMBER"

  override def superTypeOf(other: CypherType): Boolean = other match {
    case CTNumber => true
    case CTInteger => true
    case CTFloat => true
    case CTVoid => true
    case _ => false
  }

  override def joinMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case CTNumber => self
    case CTInteger => self
    case CTFloat => self
    case CTVoid => self
    case _ => CTAny
  }
}

case object CTInteger extends MaterialDefiniteCypherLeafType {

  self =>

  override def name = "INTEGER"

  override def joinMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case CTNumber => CTNumber
    case CTInteger => self
    case CTFloat => CTNumber
    case CTVoid => self
    case _ => CTAny
  }
}

case object CTFloat extends MaterialDefiniteCypherLeafType {

  self =>

  override def name = "FLOAT"

  override def joinMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case CTNumber => CTNumber
    case CTInteger => CTNumber
    case CTFloat => self
    case CTVoid => self
    case _ => CTAny
  }
}

case object CTString extends MaterialDefiniteCypherLeafType {
  override def name = "STRING"
}

case class CTMap(innerTypes: Map[String, CypherType]) extends MaterialDefiniteCypherType with MaterialDefiniteCypherType.DefaultOrNull {
  override def name = {
    val innerNames = innerTypes.map {
      case(key, valueType) => s"$key: ${valueType.name}"
    }.mkString(", ")

    s"MAP($innerNames)"
  }

  override def superTypeOf(other: CypherType): Boolean = other match {
    case CTMap(otherInner) if innerTypes.keySet == otherInner.keySet =>
      innerTypes.forall { case (key, value) => value.superTypeOf(otherInner(key)) }
    case CTVoid => true
    case _ => false
  }

  override def joinMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case CTMap(otherInnerTypes: Map[String, CypherType]) => CTMap(innerTypes |+| otherInnerTypes)
    case CTVoid => this
    case _ => CTAny
  }
}

sealed trait TemporalValueCypherType extends MaterialDefiniteCypherLeafType
sealed trait TemporalInstantCypherType extends TemporalValueCypherType

case object CTLocalDateTime extends TemporalInstantCypherType {
  override def name = "LOCALDATETIME"
}

case object CTDate extends TemporalInstantCypherType {
  override def name = "DATE"
}

case object CTDuration extends TemporalValueCypherType {
  override def name = "DURATION"
}

case object CTIdentity extends MaterialDefiniteCypherLeafType {
  override def name = "IDENTITY"
}

object CTNode extends CTNode(Set.empty, None) with Serializable {
  def apply(labels: String*): CTNode =
    CTNode(labels.toSet)

  def apply(labels: Set[String]): CTNode =
    CTNode(labels, None)
}

sealed case class CTNode(
  labels: Set[String],
  override val graph: Option[QualifiedGraphName]
) extends MaterialDefiniteCypherType {

  self =>

  private def graphToString = graph.map(n => s" @ $n").getOrElse("")

  final override def name: String =
    if (labels.isEmpty) s"NODE$graphToString" else s"NODE(${labels.mkString(":", ":", "")})$graphToString"

  final override def nullable: CTNodeOrNull = CTNodeOrNull(labels, graph)

  final override def superTypeOf(other: CypherType): Boolean = other match {
    case CTNode(otherLabels, _) => labels subsetOf otherLabels
    case CTVoid => true
    case _ => false
  }

  final override def joinMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case CTNode(otherLabels, qgnOpt) => CTNode(labels intersect otherLabels, if (graph == qgnOpt) graph else None)
    case CTVoid => self
    case _ => CTAny
  }

  final override def meetMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case CTNode(otherLabels, qgnOpt) => CTNode(labels union otherLabels, if (graph == qgnOpt) graph else None)
    case _ => super.meetMaterially(other)
  }

  override def withoutGraph: CypherType = copy(graph = None)
}

object CTNodeOrNull extends CTNodeOrNull(Set.empty, None) with Serializable {
  def apply(labels: String*): CTNodeOrNull =
    CTNodeOrNull(labels.toSet)

  def apply(labels: Set[String]): CTNodeOrNull =
    CTNodeOrNull(labels, None)
}

sealed case class CTNodeOrNull(
  labels: Set[String],
  override val graph: Option[QualifiedGraphName]
) extends NullableDefiniteCypherType {
  final override def name = s"$material?"

  final override def material: CTNode = CTNode(labels, graph)
}

object CTRelationship extends CTRelationship(Set.empty, None) with Serializable {
  def apply(types: String*): CTRelationship =
    CTRelationship(types.toSet)

  def apply(types: Set[String]): CTRelationship =
    CTRelationship(types, None)
}

sealed case class CTRelationship(
  types: Set[String],
  override val graph: Option[QualifiedGraphName]
) extends MaterialDefiniteCypherType {

  self =>

  private def graphToString = graph.map(n => s" @ $n").getOrElse("")

  final override def name: String =
    if (types.isEmpty) s"RELATIONSHIP$graphToString" else s"RELATIONSHIP(${
      types.map(t => s"$t").mkString(":", "|:", "")
    })$graphToString"

  final override def nullable: CTRelationshipOrNull =
    CTRelationshipOrNull(types, graph)

  final override def superTypeOf(other: CypherType): Boolean = other match {
    case CTRelationship(_, _) if types.isEmpty => true
    case CTRelationship(otherTypes, _) if otherTypes.isEmpty => false
    case CTRelationship(otherTypes, _) => otherTypes subsetOf types
    case CTVoid => true
    case _ => false
  }

  final override def joinMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case CTRelationship(otherTypes, qgnOpt) =>
      if (types.isEmpty || otherTypes.isEmpty)
        CTRelationship(Set.empty[String], if (graph == qgnOpt) graph else None)
      else
        CTRelationship(types union otherTypes, if (graph == qgnOpt) graph else None)
    case CTVoid => self
    case _ => CTAny
  }

  final override def meetMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case CTRelationship(otherTypes, qgnOpt) =>
      if (types.isEmpty) other
      else if (otherTypes.isEmpty) self
      else {
        val sharedTypes = types intersect otherTypes
        if (sharedTypes.isEmpty) CTVoid else CTRelationship(sharedTypes, if (graph == qgnOpt) graph else None)
      }

    case _ =>
      super.meetMaterially(other)
  }

  override def withoutGraph: CypherType = copy(graph = None)
}

object CTRelationshipOrNull extends CTRelationshipOrNull(Set.empty, None) with Serializable {
  def apply(types: String*): CTRelationshipOrNull =
    CTRelationshipOrNull(types.toSet)

  def apply(types: Set[String]): CTRelationshipOrNull =
    CTRelationshipOrNull(types, None)
}

sealed case class CTRelationshipOrNull(
  types: Set[String],
  override val graph: Option[QualifiedGraphName]
) extends NullableDefiniteCypherType {
  final override def name = s"$material?"

  final override def material: CTRelationship =
    CTRelationship(types, graph)
}

case object CTPath extends MaterialDefiniteCypherLeafType {
  override def name = "PATH"
}

final case class CTList(elementType: CypherType) extends MaterialDefiniteCypherType {

  self =>

  override def graph: Option[QualifiedGraphName] = elementType.graph

  override def name = s"LIST($elementType)"

  override def nullable =
    CTListOrNull(elementType)

  override def containsNullable: Boolean = elementType.containsNullable

  override def superTypeOf(other: CypherType): Boolean = other match {
    case CTList(otherEltType) => elementType superTypeOf otherEltType
    case CTVoid => true
    case _ => false
  }

  override def joinMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case CTList(otherEltType) => CTList(elementType join otherEltType)
    case CTVoid => self
    case _ => CTAny
  }

  override def meetMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case CTList(otherEltType) => CTList(elementType meet otherEltType)
    case _ => super.meetMaterially(other)
  }
}

final case class CTListOrNull(eltType: CypherType) extends NullableDefiniteCypherType {
  override def name = s"LIST($eltType)?"

  override def material =
    CTList(eltType)

}

case object CTVoid extends MaterialDefiniteCypherType {

  self =>

  override def name = "VOID"

  override def nullable: CTNull.type = CTNull

  override def superTypeOf(other: CypherType): Boolean = other match {
    case _ if self == other => true
    case CTVoid => true
    case _ => false
  }

  override def joinMaterially(other: MaterialCypherType): MaterialCypherType = other

  override def meetMaterially(other: MaterialCypherType): MaterialCypherType = self
}

case object CTNull extends NullableDefiniteCypherType {
  override def name = "NULL"

  override def material: CTVoid.type = CTVoid
}

sealed trait CypherType extends Serializable {
  self =>

  // We distinguish types in a 4x4 matrix
  //
  // (I) nullable (includes null) vs material
  //

  // true, if null is a value of this type
  def isNullable: Boolean

  def graph: Option[QualifiedGraphName] = None

  def withoutGraph: CypherType = this

  final override def toString: String = name

  def name: String

  // identical type that additionally includes null
  def nullable: NullableCypherType

  // identical type that additionally does not include null
  def material: MaterialCypherType

  // returns this type with the same 'nullability' (i.e. either material or nullable) as typ
  final def asNullableAs(typ: CypherType): CypherType =
    if (typ.isNullable) nullable else material

  // true, if this type or any of its type parameters include null
  def containsNullable: Boolean = isNullable

  /** join == union type == smallest shared super type */
  final def join(other: CypherType): CypherType = {
    val joined = self.material joinMaterially other.material
    if (self.isNullable || other.isNullable) joined.nullable else joined
  }

  /** meet == intersection type == largest shared sub type */
  final def meet(other: CypherType): CypherType = {
    val met = self.material meetMaterially other.material
    if (self.isNullable && other.isNullable) met.nullable else met
  }

  final def couldBeSameTypeAs(other: CypherType): Boolean = {
    self.superTypeOf(other) || self.subTypeOf(other)
  }

  final def intersects(other: CypherType): Boolean =
    meet(other) != CTVoid

  final def subTypeOf(other: CypherType): Boolean =
    other superTypeOf self

  /**
    * A type U is a super type of a type V iff it holds
    * that any value of type V is also a value of type U
    *
    * @return true if this type is a super type of other
    */
  def superTypeOf(other: CypherType): Boolean
}

sealed trait MaterialCypherType extends CypherType {
  self: CypherType =>

  final override def isNullable = false

  def joinMaterially(other: MaterialCypherType): MaterialCypherType

  def meetMaterially(other: MaterialCypherType): MaterialCypherType =
    if (self superTypeOf other) other
    else if (other superTypeOf self) self
    else CTVoid

}

sealed trait NullableCypherType extends CypherType {
  self =>

  final override def isNullable = true

  override def superTypeOf(other: CypherType): Boolean =
    material superTypeOf other.material
}

sealed trait DefiniteCypherType {
  self: CypherType =>

  override def nullable: NullableCypherType with DefiniteCypherType

  override def material: MaterialCypherType with DefiniteCypherType

}

private[okapi] object MaterialDefiniteCypherType {

  sealed private[okapi] trait DefaultOrNull {
    self: MaterialDefiniteCypherType =>

    override val nullable: NullableDefiniteCypherType = self match {
      // TODO: figure out why the previous anonymous class impl here sometimes didn't work
      // it didn't return the singleton on .material, and in some cases was not
      // equal to another instance of itself
      case CTString => CTStringOrNull
      case CTInteger => CTIntegerOrNull
      case CTBoolean => CTBooleanOrNull
      case CTAny => CTAnyOrNull
      case CTNumber => CTNumberOrNull
      case CTFloat => CTFloatOrNull
      case CTMap(inner) => CTMapOrNull(inner)
      case CTLocalDateTime => CTLocalDateTimeOrNull
      case CTDate => CTDateOrNull
      case CTDuration => CTDurationOrNull
      case CTPath => CTPathOrNull
      case CTIdentity => CTIdentityOrNull
    }
  }

}

case object CTIntegerOrNull extends NullableDefiniteCypherType {
  override def name: String = CTInteger + "?"

  override def material: CTInteger.type = CTInteger
}

case object CTStringOrNull extends NullableDefiniteCypherType {
  override def name: String = CTString + "?"

  override def material: CTString.type = CTString
}

case object CTBooleanOrNull extends NullableDefiniteCypherType {
  override def name: String = CTBoolean + "?"

  override def material: CTBoolean.type = CTBoolean
}

case object CTAnyOrNull extends NullableDefiniteCypherType {
  override def name: String = CTAny + "?"

  override def material: CTAny.type = CTAny
}

case object CTNumberOrNull extends NullableDefiniteCypherType {
  override def name: String = CTNumber + "?"

  override def material: CTNumber.type = CTNumber
}

case object CTFloatOrNull extends NullableDefiniteCypherType {
  override def name: String = CTFloat + "?"

  override def material: CTFloat.type = CTFloat
}

case object CTLocalDateTimeOrNull extends NullableDefiniteCypherType {
  override def name: String = CTLocalDateTime + "?"

  override def material: CTLocalDateTime.type  = CTLocalDateTime
}

case object CTDateOrNull extends NullableDefiniteCypherType {
  override def name: String = CTDate + "?"

  override def material: CTDate.type = CTDate
}

case object CTDurationOrNull extends NullableDefiniteCypherType {
  override def name: String = CTDuration + "?"

  override def material: CTDuration.type  = CTDuration
}

case class CTMapOrNull(innerTypes: Map[String, CypherType]) extends NullableDefiniteCypherType {
  override def name: String = CTMap(innerTypes) + "?"

  override def material: CTMap = CTMap(innerTypes)
}

case object CTPathOrNull extends NullableDefiniteCypherType {
  override def name: String = CTPath + "?"

  override def material: CTPath.type = CTPath
}

case object CTIdentityOrNull extends NullableDefiniteCypherType {
  override def name: String = CTIdentity + "?"

  override def material: CTIdentity.type = CTIdentity
}

sealed private[okapi] trait MaterialDefiniteCypherType extends MaterialCypherType with DefiniteCypherType {
  self =>

  override def material: MaterialDefiniteCypherType = self

}

sealed private[okapi] trait NullableDefiniteCypherType extends NullableCypherType with DefiniteCypherType {
  self =>

  override def nullable: NullableDefiniteCypherType = self

}

sealed private[okapi] trait MaterialDefiniteCypherLeafType
  extends MaterialDefiniteCypherType
    with MaterialDefiniteCypherType.DefaultOrNull {

  self =>

  override def superTypeOf(other: CypherType): Boolean = other match {
    case _ if self == other => true
    case CTVoid => true
    case _ => false
  }

  override def joinMaterially(other: MaterialCypherType): MaterialCypherType = other match {
    case _ if self == other => self
    case CTVoid => self
    case _ => CTAny
  }
}
