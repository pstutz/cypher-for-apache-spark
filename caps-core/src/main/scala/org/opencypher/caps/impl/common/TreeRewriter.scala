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

import scala.reflect.ClassTag

abstract class TreeRewriter[I <: TreeNode[I]: ClassTag, O <: TreeNode[O]: ClassTag] {
  def rewrite(tree: I): O
}

/**
  * Applies the given partial function starting from the leafs of this tree.
  */
case class BottomUp[T <: TreeNode[T]: ClassTag](rule: PartialFunction[T, T]) extends TreeRewriter[T, T] {

  def rewrite(tree: T): T = {
    val childrenLength = tree.children.length
    val afterChildren = if (childrenLength == 0) {
      tree
    } else {
      val updatedChildren = {
        val childrenCopy = new Array[T](childrenLength)
        var i = 0
        while (i < childrenLength) {
          childrenCopy(i) = rewrite(tree.children(i))
          i += 1
        }
        childrenCopy
      }
      tree.withNewChildren(updatedChildren)
    }
    if (rule.isDefinedAt(afterChildren)) rule(afterChildren) else afterChildren
  }

}

/**
  * Applies the given partial function starting from the leafs of this tree.
  *
  * Avoids using the stack.
  */
final case class OffStackBottomUp[T <: TreeNode[T]: ClassTag](rule: PartialFunction[T, T]) extends TreeRewriter[T, T] {

  def depthFirstTraversal(tree: T): List[T] = {
    var result = List.empty[T]
    var nodesToProcess: List[T] = List[T](tree)
    while (!nodesToProcess.isEmpty) {
      val head :: tail = nodesToProcess
      result = head :: result
      nodesToProcess = tail ::: head.children.toList
    }
    result
  }

  def rewrite(in: T): T = {
    val nodesToRewrite = depthFirstTraversal(in).iterator
    var rewrittenNodes = List.empty[T]
    while (nodesToRewrite.hasNext) {
      val nextNodeToRewrite = nodesToRewrite.next
      if (nextNodeToRewrite.children.isEmpty) {
        val rewrittenNode = if (rule.isDefinedAt(nextNodeToRewrite)) rule(nextNodeToRewrite) else nextNodeToRewrite
        rewrittenNodes = rewrittenNode :: rewrittenNodes
      } else {
        val (rewrittenChildren, remainingRewritten) = rewrittenNodes.splitAt(nextNodeToRewrite.children.length)
        val updatedNextNode = nextNodeToRewrite.withNewChildren(rewrittenChildren.toArray)
        val rewrittenNextNode =
          if (rule.isDefinedAt(updatedNextNode)) rule(updatedNextNode) else updatedNextNode
        rewrittenNodes = rewrittenNextNode :: remainingRewritten
      }
    }
    assert(rewrittenNodes.size == 1)
    rewrittenNodes.head
  }

}

/**
  * Applies the given partial function starting from the root of this tree.
  *
  * @note Note the applied rule cannot insert new parent nodes.
  */
case class TopDown[T <: TreeNode[T]: ClassTag](rule: PartialFunction[T, T]) extends TreeRewriter[T, T] {

  def rewrite(tree: T): T = {
    val afterSelf = if (rule.isDefinedAt(tree)) rule(tree) else tree
    val childrenLength = afterSelf.children.length
    if (childrenLength == 0) {
      afterSelf
    } else {
      val updatedChildren = {
        val childrenCopy = new Array[T](childrenLength)
        var i = 0
        while (i < childrenLength) {
          childrenCopy(i) = rewrite(afterSelf.children(i))
          i += 1
        }
        childrenCopy
      }
      afterSelf.withNewChildren(updatedChildren)
    }
  }

}

/**
  * Applies the given partial function starting from the root of this tree.
  *
  * Avoids using the stack.
  */
final case class OffStackTopDown[T <: TreeNode[T]: ClassTag](rule: PartialFunction[T, T]) extends TreeRewriter[T, T] {

  def depthFirstTraversal(tree: T): List[T] = {
    var result = List.empty[T]
    var nodesToProcess: List[T] = List[T](tree)
    while (!nodesToProcess.isEmpty) {
      val head :: tail = nodesToProcess
      result = head :: result
      nodesToProcess = tail ::: head.children.toList
    }
    result
  }

  def rewrite(in: T): T = {
    var nodesToRewrite = depthFirstTraversal(in)
    var rewrittenNodes = List.empty[T]
    while (!nodesToRewrite.isEmpty) {
      val nextNodeToRewrite :: remainingNodesToRewrite = nodesToRewrite
      nodesToRewrite = remainingNodesToRewrite
      if (nextNodeToRewrite.children.isEmpty) {
        val rewrittenNode = if (rule.isDefinedAt(nextNodeToRewrite)) rule(nextNodeToRewrite) else nextNodeToRewrite
        rewrittenNodes = rewrittenNode :: rewrittenNodes
      } else {
        val (rewrittenChildren, remainingRewritten) = rewrittenNodes.splitAt(nextNodeToRewrite.children.length)
        val rewrittenNextNode =
          if (rule.isDefinedAt(updatedNextNode)) rule(updatedNextNode) else updatedNextNode
        val updatedNextNode = nextNodeToRewrite.withNewChildren(rewrittenChildren.toArray)
        rewrittenNodes = rewrittenNextNode :: remainingRewritten
      }
    }
    assert(rewrittenNodes.size == 1)
    rewrittenNodes.head
  }

}
