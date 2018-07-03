/*
 * Copyright (c) 2016-2018 "Neo4j Sweden, AB" [https://neo4j.com]
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
package org.opencypher.okapi.trees

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import scala.util.hashing.MurmurHash3
import reflect.runtime.universe._
import reflect.runtime.{currentMirror, universe}

/**
  * This is the basic tree node class. Usually it makes more sense to use `AbstractTreeNode`, which uses reflection
  * to generate the `children` and `withNewChildren` field/method. Our benchmarks show that manually implementing
  * them can result in a speedup of a factor of ~3 for tree rewrites, so in performance critical places it might
  * make sense to extend `TreeNode` instead.
  *
  * This class uses array operations instead of Scala collections, both for improved performance as well as to save
  * stack frames during recursion, which allows it to operate on trees that are several thousand nodes high.
  */
abstract class TreeNode[T <: TreeNode[T] : ClassTag : TypeTag] extends Product with Traversable[T] {
  self: T =>

  def withNewChildren(newChildren: Array[T]): T

  def children: Array[T] = Array.empty

  override def hashCode: Int = MurmurHash3.productHash(self)

  def arity: Int = children.length

  def isLeaf: Boolean = height == 1

  def height: Int = {
    val childrenLength = children.length
    var i = 0
    var result = 0
    while (i < childrenLength) {
      result = math.max(result, children(i).height)
      i += 1
    }
    result + 1
  }

  override def size: Int = {
    val childrenLength = children.length
    var i = 0
    var result = 1
    while (i < childrenLength) {
      result += children(i).size
      i += 1
    }
    result
  }

  def map[O <: TreeNode[O] : ClassTag](f: T => O): O = {
    val childrenLength = children.length
    if (childrenLength == 0) {
      f(self)
    } else {
      val mappedChildren = new Array[O](childrenLength)
      var i = 0
      while (i < childrenLength) {
        mappedChildren(i) = f(children(i))
        i += 1
      }
      f(self).withNewChildren(mappedChildren)
    }
  }

  override def foreach[O](f: T => O): Unit = {
    f(this)
    val childrenLength = children.length
    var i = 0
    while (i < childrenLength) {
      children(i).foreach(f)
      i += 1
    }
  }

  /**
    * Checks if the parameter tree is contained within this tree. A tree always contains itself.
    *
    * @param other other tree
    * @return true, iff `other` is contained in that tree
    */
  def containsTree(other: T): Boolean = {
    if (self == other) {
      true
    } else {
      val childrenLength = children.length
      var i = 0
      while (i < childrenLength && !children(i).containsTree(other)) i += 1
      i != childrenLength
    }
  }

  /**
    * Checks if `other` is a direct child of this tree.
    *
    * @param other other tree
    * @return true, iff `other` is a direct child of this tree
    */
  def containsChild(other: T): Boolean = {
    children.contains(other)
  }

  /**
    * Returns a string-tree representation of the node.
    *
    * @return tree-style representation of that node and all children
    */
  def pretty: String = {
    val lines = new ArrayBuffer[String]

    @tailrec
    def recTreeToString(toPrint: List[T], prefix: String, stack: List[List[T]]): Unit = {
      toPrint match {
        case Nil =>
          stack match {
            case Nil =>
            case top :: remainingStack =>
              recTreeToString(top, prefix.dropRight(4), remainingStack)
          }
        case last :: Nil =>
          lines += s"$prefix╙──${last.toString}"
          recTreeToString(last.children.toList, s"$prefix    ", Nil :: stack)
        case next :: siblings =>
          lines += s"$prefix╟──${next.toString}"
          recTreeToString(next.children.toList, s"$prefix║   ", siblings :: stack)
      }
    }

    recTreeToString(List(this), "", Nil)
    lines.mkString("\n")
  }

  /**
    * Prints a tree representation of the node.
    */
  def show(): Unit = {
    println(pretty)
  }

  /**
    * Turns all arguments in `args` into a string that describes the arguments.
    *
    * @return argument string
    */
  def argString: String = args.mkString(", ")

  /**
    * Arguments that should be printed. The default implementation excludes children.
    */
  def args: Iterator[Any] = {
    currentMirror.reflect(this)
      .symbol
      .typeSignature
      .members
      .toList
      .collect { case a: TermSymbol => a }
      .filterNot(_.isLazy)
      .filterNot(_.isMethod)
      .filterNot(_.isModule)
      .filterNot(_.isClass)
      .map(currentMirror.reflect(this).reflectField)
      .map(termSymbol => termSymbol -> termSymbol.get)
      .filter { case (termSymbol, value) =>
        value match {
          case c: T if containsChild(c) => false // Don't print children
          case _: Option[_] =>
            val innerType = termSymbol.symbol.typeSignature.typeArgs.head
            if (innerType <:< typeOf[T]) {
              false // Don't print children
            } else {
              true
            }
          case _: Iterable[_] =>
            val innerType = termSymbol.symbol.typeSignature.typeArgs.head
            if (innerType <:< typeOf[T]) {
              false // Don't print children
            } else {
              true
            }
          case _: Array[T] => false // Don't print children
          case _ => true
        }
      }
      .map { case (termSymbol, value) => s"${termSymbol.symbol.name.toString.trim}=$value" }
      .reverseIterator
  }

  override def toString = s"${getClass.getSimpleName}${if (argString.isEmpty) "" else s"($argString)"}"
}
