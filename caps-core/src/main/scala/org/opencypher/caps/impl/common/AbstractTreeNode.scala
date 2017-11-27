/*
 * Copyright (c) 2016-2017 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.caps.impl.common

import org.opencypher.caps.impl.common.AbstractTreeNode.cachedCopyMethods
import org.opencypher.caps.impl.spark.exception.Raise

import scala.reflect.ClassTag

/**
  * Class that implements the `children` and `withNewChildren` methods using reflection when implementing
  * `TreeNode` with a case class or case object.
  *
  * Requirements: All child nodes need to be individual constructor parameters and their order
  * in children is their order in the constructor. Every constructor parameter of type `T` is
  * assumed to be a child node.
  */
abstract class AbstractTreeNode[T <: TreeNode[T] : ClassTag] extends TreeNode[T] {
  self: T =>

  override final val children: Array[T] = {
    val constructorParamLength = productArity
    val childrenArray = new Array[T](constructorParamLength)
    var i = 0
    var ci = 0
    while (i < constructorParamLength) {
      val pi = productElement(i)
      pi match {
        case c: T =>
          childrenArray(ci) = c
          ci += 1
        case _ =>
      }
      i += 1
    }
    val properSizedChildren = new Array[T](ci)
    System.arraycopy(childrenArray, 0, properSizedChildren, 0, ci)
    properSizedChildren
  }

  /**
    * Cache children as a set for faster rewrites.
    */
  override final lazy val childrenAsSet = children.toSet

  @inline override final def withNewChildren(newChildren: Array[T]): T = {
    if (sameAsCurrentChildren(newChildren)) {
      self
    } else {
      val updatedConstructorParams = updateConstructorParams(newChildren)
      val copyMethod = AbstractTreeNode.copyMethod(self)
      try {
        copyMethod(updatedConstructorParams: _*).asInstanceOf[T]
      } catch {
        case e: Exception =>
          Raise.invalidArgument(
            s"valid constructor arguments for $productPrefix",
            s"""|${updatedConstructorParams.mkString(", ")}
                |Original exception: $e
                |Copy method: $copyMethod""".stripMargin
          )
      }
    }
  }

  @inline private final def updateConstructorParams(newChildren: Array[T]): Array[Any] = {
    require(
      children.length == newChildren.length,
      s"invalid children for $productPrefix: ${newChildren.mkString(", ")}")
    val parameterArrayLength = productArity
    val parameterArray = new Array[Any](parameterArrayLength)
    var productIndex = 0
    var childrenIndex = 0
    while (productIndex < parameterArrayLength) {
      val currentProductElement = productElement(productIndex)
      if (currentProductElement == children(childrenIndex)) {
        parameterArray(productIndex) = newChildren(childrenIndex)
        childrenIndex += 1
      } else {
        parameterArray(productIndex) = currentProductElement
      }
      productIndex += 1
    }
    parameterArray
  }

  @inline private final def sameAsCurrentChildren(newChildren: Array[T]): Boolean = {
    val childrenLength = children.length
    if (childrenLength != newChildren.length) {
      false
    } else {
      var i = 0
      while (i < childrenLength) {
        if (children(i) != newChildren(i)) return false
        i += 1
      }
      true
    }
  }

}

/**
  * Caches an instance of the copy method per case class type.
  */
object AbstractTreeNode {

  import scala.reflect.runtime.universe
  import scala.reflect.runtime.universe._

  // No synchronization required: no problem if a cache entry is lost due to a concurrent write.
  @volatile private var cachedCopyMethods = Map.empty[Class[_], MethodMirror]

  private final lazy val mirror = universe.runtimeMirror(getClass.getClassLoader)

  @inline protected final def copyMethod(instance: AbstractTreeNode[_]): MethodMirror = {
    val instanceClass = instance.getClass
    cachedCopyMethods.getOrElse(
      instanceClass, {
        val copyMethod = reflectCopyMethod(instance)
        cachedCopyMethods = cachedCopyMethods.updated(instanceClass, copyMethod)
        copyMethod
      }
    )
  }

  @inline private final def reflectCopyMethod(instance: Object): MethodMirror = {
    val instanceMirror = mirror.reflect(instance)
    val tpe = instanceMirror.symbol.asType.toType
    val copyMethodSymbol = tpe.decl(TermName("copy")).asMethod
    instanceMirror.reflectMethod(copyMethodSymbol)
  }

}
