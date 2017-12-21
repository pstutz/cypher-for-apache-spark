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

import scala.annotation.tailrec
import scala.collection.mutable
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
  * Avoids using the stack
  */
case class OffStackBottomUp[T <: TreeNode[T]: ClassTag](rule: PartialFunction[T, T]) extends TreeRewriter[T, T] {

  def rewrite(in: T): T = {
    @tailrec def offStackRewrite(tree: T, stack: List[Array[T]]): T = {

      val childrenLength = tree.children.length
      val afterChildren = if (childrenLength == 0) {
        tree
      } else {
        val updatedChildren = {
          val childrenCopy = new Array[T](childrenLength)
          var i = 0
          while (i < childrenLength) {
            childrenCopy(i) = offStackRewrite(tree.children(i))

            i += 1
          }
          childrenCopy
        }
        tree.withNewChildren(updatedChildren)
      }
      if (rule.isDefinedAt(afterChildren)) rule(afterChildren) else afterChildren
    }

    offStackRewrite(in)
  }

//  @tailrec
//  private def rec(stack: mutable.ArrayStack[(List[AnyRef], mutable.MutableList[AnyRef])]): mutable.MutableList[AnyRef] = {
//    val (currentJobs, _) = stack.top
//    if (currentJobs.isEmpty) {
//      val (_, newChildren) = stack.pop()
//      if (stack.isEmpty) {
//        newChildren
//      } else {
//        val (job :: jobs, doneJobs) = stack.pop()
//        val doneJob = job.dup(newChildren)
//        val rewrittenDoneJob = doneJob.rewrite(rewriter)
//        stack.push((jobs, doneJobs += rewrittenDoneJob))
//        rec(stack)
//      }
//    } else {
//      val next = currentJobs.head
//      if (stopper(next)) {
//        val (job :: jobs, doneJobs) = stack.pop()
//        stack.push((jobs, doneJobs += job))
//      } else {
//        stack.push((next.children.toList, new mutable.MutableList()))
//      }
//      rec(stack)
//    }
//  }
//}

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
