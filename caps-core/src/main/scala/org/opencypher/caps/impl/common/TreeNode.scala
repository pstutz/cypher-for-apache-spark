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

import java.util.concurrent.RecursiveTask

import scala.reflect.ClassTag
import scala.util.hashing.MurmurHash3

abstract class TreeNode[T <: TreeNode[T] : ClassTag] extends Product with Traversable[T] {

  self: T =>

  def withNewChildren(newChildren: Array[T]): T

  def children: Array[T] = Array.empty[T]

  /**
    * Explicit accessor to the set of children. This allows for an implementation to cache this,
    * which can speed up rewrites.
    */
  def childrenAsSet: Set[T] = children.toSet

  // Optimization: Cache hash code, speeds up repeated computations over large trees.
  override final lazy val hashCode: Int = MurmurHash3.productHash(self)

  override final lazy val size: Int = {
    val childrenLength = children.length
    var i = 0
    var result = 1
    while (i < childrenLength) {
      result += children(i).size
      i += 1
    }
    result
  }

  lazy final val height: Int = {
    val childrenLength = children.length
    var i = 0
    var result = 0
    while (i < childrenLength) {
      result = math.max(result, children(i).height)
      i += 1
    }
    result + 1
  }

  def arity: Int = children.length

  def isLeaf: Boolean = height == 1

  @inline final def map[O <: TreeNode[O] : ClassTag](f: T => O): O = {
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

  @inline override final def foldLeft[O](initial: O)(f: (O, T) => O): O = {
    children.foldLeft(f(initial, this)) {
      case (agg, nextChild) =>
        nextChild.foldLeft(agg)(f)
    }
  }

  @inline override final def foreach[O](f: T => O): Unit = {
    f(this)
    children.foreach(_.foreach(f))
  }

  /**
    * Checks if the parameter tree is contained within this tree. A tree always contains itself.
    *
    * @param other other tree
    * @return true, iff `other` is contained in that tree
    */
  @inline final def containsTree(other: T): Boolean = {
    if (self == other) true else children.exists(_.containsTree(other))
  }

  /**
    * Checks if `other` is a direct child of this tree.
    *
    * @param other other tree
    * @return true, iff `other` is a direct child of this tree
    */
  @inline final def containsChild(other: T): Boolean = {
    childrenAsSet.contains(other)
  }

  /**
    * Applies the given partial function starting from the
    * leafs of this tree.
    *
    * @param rule rewrite rule
    * @return rewritten tree
    */
  @inline final def transformUp(rule: PartialFunction[T, T]): T = {
    val childrenLength = children.length
    val afterChildren = if (childrenLength == 0) {
      self
    } else {
      val updatedChildren = {
        val childrenCopy = new Array[T](childrenLength)
        var i = 0
        while (i < childrenLength) {
          childrenCopy(i) = children(i).transformUp(rule)
          i += 1
        }
        childrenCopy
      }
      withNewChildren(updatedChildren)
    }
    if (rule.isDefinedAt(afterChildren)) rule(afterChildren) else afterChildren
  }

  /**
    * Applies the given partial function starting from the
    * root of this tree.
    *
    * @note Note that the applied rule must not insert new parent nodes.
    * @param rule rewrite rule
    * @return rewritten tree
    */
  @inline final def transformDown(rule: PartialFunction[T, T]): T = {
    val afterSelf = if (rule.isDefinedAt(self)) rule(self) else self
    val childrenLength = afterSelf.children.length
    if (childrenLength == 0) {
      afterSelf
    } else {
      val updatedChildren = {
        val childrenCopy = new Array[T](childrenLength)
        var i = 0
        while (i < childrenLength) {
          childrenCopy(i) = afterSelf.children(i).transformDown(rule)
          i += 1
        }
        childrenCopy
      }
      afterSelf.withNewChildren(updatedChildren)
    }
  }

  /**
    * Prints the tree node and its children recursively in a tree-style layout.
    *
    * @param depth indentation depth used by the recursive call
    * @return tree-style representation of that node and all (grand-)children
    */
  def pretty(implicit depth: Int = 0): String = {

    def prefix(depth: Int): String = ("· " * depth) + "|-"

    val childrenString = children.foldLeft(new StringBuilder()) {
      case (agg, s) => agg.append(s.pretty(depth + 1))
    }
    s"${prefix(depth)}$self\n$childrenString"
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
  def args: Iterator[Any] = productIterator.flatMap {
    case tn: T if containsChild(tn) => None // Don't print children
    case other => Some(other.toString)
  }

  override def toString = s"${getClass.getSimpleName}${if (argString.isEmpty) "" else s"($argString)"}"
}
