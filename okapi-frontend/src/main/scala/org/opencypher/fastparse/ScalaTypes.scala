package org.opencypher.fastparse

import StringUtilities._

trait ScalaType {
  def name: String

  def nameAsParameter: String

  def typeSignature: String

  def asParameter: String = s"$nameAsParameter: $typeSignature"

  def scalaClassDef: Option[String] = None

  def withParameterName(parameterName: String): ScalaType
}

case class StringType(maybeNameAsParameter: Option[String] = None) extends ScalaType {
  override def name: String = "String"

  override def nameAsParameter: String = maybeNameAsParameter.getOrElse(name.asParamName)

  override def typeSignature: String = name

  def withParameterName(parameterName: String): ScalaType = copy(maybeNameAsParameter = Some(parameterName))
}

case class BooleanType(maybeNameAsParameter: Option[String] = None) extends ScalaType {
  override def name: String = "Boolean"

  override def nameAsParameter: String = maybeNameAsParameter.getOrElse(name.asParamName)

  override def typeSignature: String = name

  def withParameterName(parameterName: String): ScalaType = copy(maybeNameAsParameter = Some(parameterName))
}

case class OptionType(elementType: ScalaType, maybeNameAsParameter: Option[String] = None) extends ScalaType {
  override def name: String = "Option"

  override def nameAsParameter: String = maybeNameAsParameter.getOrElse(s"maybe${elementType.nameAsParameter.firstCharToUpperCase}")

  override def typeSignature: String = s"Option[${elementType.typeSignature}]"

  def withParameterName(parameterName: String): ScalaType = copy(maybeNameAsParameter = Some(parameterName))
}

case class TupleType(parameters: List[ScalaType], maybeNameAsParameter: Option[String] = None) extends ScalaType {
  def name = s"Tuple${parameters.size}"

  override def nameAsParameter: String = maybeNameAsParameter.getOrElse(name.asParamName)

  override def typeSignature: String = s"(${parameters.map(_.typeSignature).mkString(", ")})"

  def withParameterName(parameterName: String): ScalaType = copy(maybeNameAsParameter = Some(parameterName))
}

case class ListType(elementType: ScalaType, maybeNameAsParameter: Option[String] = None) extends ScalaType {
  override def name: String = "List"

  override def nameAsParameter: String = maybeNameAsParameter.getOrElse(s"${elementType.nameAsParameter}s")

  override def typeSignature: String = s"List[${elementType.typeSignature}]"

  def withParameterName(parameterName: String): ScalaType = copy(maybeNameAsParameter = Some(parameterName))
}

case class TraitType(name: String, maybeNameAsParameter: Option[String] = None) extends ScalaType {
  //inheritance: List[TraitType],
  override def nameAsParameter: String = maybeNameAsParameter.getOrElse(name.asParamName)

  override def typeSignature: String = name

  override def scalaClassDef: Option[String] = Some(s"""trait $name""")

  def withParameterName(parameterName: String): ScalaType = copy(maybeNameAsParameter = Some(parameterName))
}

case class CaseClassType(name: String, parameters: List[ScalaType], maybeNameAsParameter: Option[String] = None) extends ScalaType {
  //inheritance: List[TraitType]
  override def nameAsParameter: String = maybeNameAsParameter.getOrElse(name.asParamName)

  override def typeSignature: String = name

  override def scalaClassDef: Option[String] = Some(s"""case class $name(${parameters.map(_.asParameter).mkString(", ")})""")

  def withParameterName(parameterName: String): ScalaType = copy(maybeNameAsParameter = Some(parameterName))
}
