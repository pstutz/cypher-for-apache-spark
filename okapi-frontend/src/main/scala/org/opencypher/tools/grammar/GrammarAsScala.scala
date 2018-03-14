//package org.opencypher.tools.grammar
//
//import org.opencypher.okapi.trees.AbstractTreeNode
//import org.opencypher.tools.grammar.Helpers._
//
//abstract class GrammarAsScala extends AbstractTreeNode[GrammarAsScala] {
//  def asParam: String = s"$paramName: $classAsString"
//
//  def classAsString: String
//
//  def paramName: String
//}
//
//case class CaseClassType(classAsString: String, params: GrammarAsScala) extends GrammarAsScala {
//  override def paramName: String = classAsString.asParamName
//}
//
//case class AbstractClassType(classAsString: String) extends GrammarAsScala {
//  override def paramName: String = classAsString.asParamName
//}
//
//case class StringType(name: Option[String] = None) extends GrammarAsScala {
//  override def paramName: String = name.map(n => n.asParamName).getOrElse("s")
//
//  override def classAsString: String = "String"
//}
//
//case class ListType(name: Option[String] = None, elementType: GrammarAsScala) extends GrammarAsScala {
//  override def paramName: String = name.map(n => n.firstCharToLowerCase).getOrElse(s"${elementType.paramName.firstCharToLowerCase}s")
//
//  override def classAsString: String = s"List[${elementType.classAsString}]"
//}
//
//case class ProductType(elementTypes: List[GrammarAsScala]) extends GrammarAsScala {
//
//  override def paramName: String = {
//    elementTypes match {
//      case Nil => throw new Exception("Empty list")
//      case h :: _ => h.paramName
//    }
//  }
//
//  override def asParam: String = elementTypes.map(_.asParam).mkString(", ")
//
//  override def classAsString: String = {
//    if (elementTypes.length == 1) {
//      s"${elementTypes.head.classAsString}"
//    } else {
//      s"(${elementTypes.map(_.classAsString).mkString(", ")})"
//    }
//  }
//}
//
//case class OptionType(name: Option[String] = None, elementType: GrammarAsScala) extends GrammarAsScala {
//
//  override def asParam: String = {
//    elementType match {
//      // Special cases to turn an optional literal into a boolean flag
//      case StringType(Some(name)) => s"${name.asParamName}: Boolean"
//      case ProductType(List(StringType(Some(name)))) => s"${name.asParamName}: Boolean"
//      // Special case to get rid of complex options of lists
//      case ProductType(List(head, tail: ListType)) if head.classAsString == tail.elementType.classAsString => s"${tail.asParam}"
//      case _ => super.asParam
//    }
//  }
//
//  override def paramName: String = name.map(n => n.firstCharToLowerCase).getOrElse(s"${elementType.paramName.firstCharToLowerCase}Option")
//
//  override def classAsString: String = s"Option[${elementType.classAsString}]"
//}
