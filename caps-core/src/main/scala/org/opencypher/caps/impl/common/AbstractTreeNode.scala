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
abstract class AbstractTreeNode[T <: TreeNode[T]: ClassTag] extends TreeNode[T] {
  self: T =>

  protected override val children: Seq[T] = productIterator.collect { case t: T => t }.toArray

  /**
    * Cache children as a set for faster rewrites.
    */
  override lazy val childrenAsSet = children.toSet

  override def withNewChildren(newChildren: Seq[T]): T = {
    val newAsArray = newChildren.toArray
    if (children.length == newAsArray.length && {
          for (i <- 0 to children.length) {}

        }) {
      self
    } else {
      require(
        children.length == newAsArray.length,
        s"invalid children for $productPrefix: ${newAsVector.mkString(", ")}")
      val substitutions = children.toList.zip(newAsVector)
      val (updatedConstructorParams, _) = productIterator.foldLeft((Vector.empty[Any], substitutions)) {
        case ((result, remainingSubs), next) =>
          remainingSubs match {
            case (oldC, newC) :: tail if next == oldC => (result :+ newC, tail)
            case _                                    => (result :+ next, remainingSubs)
          }
      }
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

}

/**
  * Caches an instance of the copy method per case class type and thread.
  */
object AbstractTreeNode {

  import scala.reflect.runtime.universe
  import scala.reflect.runtime.universe._

  private lazy val cachedCopyMethods = new ThreadLocal[Map[Class[_], MethodMirror]]() {
    override def initialValue = Map.empty[Class[_], MethodMirror]
  }

  private lazy val mirror = universe.runtimeMirror(getClass.getClassLoader)

  protected def copyMethod[T <: TreeNode[T]](instance: AbstractTreeNode[T]): MethodMirror = {
    val instanceClass = instance.getClass
    val cacheMap = cachedCopyMethods.get()
    cacheMap.getOrElse(
      instanceClass, {
        val instanceMirror = mirror.reflect(instance)
        val tpe = instanceMirror.symbol.asType.toType
        val copyMethodSymbol = tpe.decl(TermName("copy")).asMethod
        val copyMethod = instanceMirror.reflectMethod(copyMethodSymbol)
        val updatedCacheMap = cacheMap.updated(instanceClass, copyMethod)
        cachedCopyMethods.set(updatedCacheMap)
        copyMethod
      }
    )
  }

}
