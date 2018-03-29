package org.opencypher

import org.opencypher.okapi.trees.AbstractTreeNode
import org.opencypher.tools.grammar.Helpers._

case class Parameter(name: String, typ: ScalaType) {
  override def toString = s"$name: $typ"
}

abstract class ScalaType extends AbstractTreeNode[ScalaType] {
  def name: String

  def superClass: Option[AbstractClassType] = None

  def asParameter: String = toString.firstCharToLowerCase

  override def toString: String = name
}

case object StringType extends ScalaType {
  def name = "String"
}

object AbstractClassType {
  def apply(name: String, superClass: AbstractClassType) = new AbstractClassType(name, Some(superClass))
}

case class AbstractClassType(name: String, override val superClass: Option[AbstractClassType] = None) extends ScalaType

case class CaseClassType(name: String, parameters: List[Parameter], override val superClass: Option[AbstractClassType]) extends ScalaType

case class ListType(elementType: ScalaType) extends ScalaType {
  override def asParameter: String = {
    val elementAsParam = elementType.asParameter
    if (elementAsParam.endsWith("s")) elementAsParam else elementAsParam + "s"
  }

  override def name: String = s"List[$elementType]"
}

case class OptionType(elementType: ScalaType) extends ScalaType {
  override def asParameter: String = s"${elementType.asParameter}Opt"

  override def name: String = s"Option[$elementType]"
}
