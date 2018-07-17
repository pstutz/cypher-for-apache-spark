package org.opencypher.okapi.trees

import cats.data.NonEmptyList

import scala.annotation.tailrec
import scala.reflect.ClassTag

sealed trait TreeOperation[T <: TreeNode[T]]

case class RewriteChildren[T <: TreeNode[T]](node: T, rewrittenChildren: List[T] = List.empty[T]) extends TreeOperation[T]

case class RewriteNode[T <: TreeNode[T]](node: T, rewrittenChildren: List[T] = List.empty[T]) extends TreeOperation[T]

case class Done[T <: TreeNode[T]](nodes: List[T]) extends TreeOperation[T]

case class BottomUpStackSafe[T <: TreeNode[T] : ClassTag](rule: PartialFunction[T, T]) extends TreeRewriter[T, T] {

  override def rewrite(tree: T): T = {

    @inline
    def updateNode(node: T): T = if (rule.isDefinedAt(node)) rule(node) else node

    @tailrec
    def stackSafeRewrite(stack: NonEmptyList[TreeOperation[T]]): T = stack match {
      case NonEmptyList(RewriteChildren(node, rewrittenChildren), tail) =>
        val updatedStack = if (node.children.isEmpty) {
          NonEmptyList(Done[T](updateNode(node) :: rewrittenChildren), tail)
        } else {
          node.children.foldLeft(NonEmptyList(RewriteNode(node, rewrittenChildren), tail)) {
            case (currentStack, child) =>
              RewriteChildren(child) :: currentStack
          }
        }
        stackSafeRewrite(updatedStack)
      case NonEmptyList(RewriteNode(node, rewrittenChildren), tail) =>
        val (currentRewrittenChildren, nextRewrittenChildren) = rewrittenChildren.splitAt(node.children.length)
        val nodeWithUpdatedChildren = node.withNewChildren(currentRewrittenChildren.toArray)
        val updatedStack = NonEmptyList(Done(updateNode(nodeWithUpdatedChildren) :: nextRewrittenChildren), tail)
        stackSafeRewrite(updatedStack)
      case NonEmptyList(Done(nodes), tail) =>
        tail match {
          case Nil =>
            assert(nodes.size == 1)
            nodes.head
          case Done(nextNodes) :: nextTail =>
            stackSafeRewrite(NonEmptyList(Done(nodes ::: nextNodes), nextTail))
          case RewriteChildren(nextNode, rewrittenChildren) :: nextTail =>
            stackSafeRewrite(NonEmptyList(RewriteChildren(nextNode, nodes ::: rewrittenChildren), nextTail))
          case RewriteNode(nextNode, rewrittenChildren) :: nextTail =>
            stackSafeRewrite(NonEmptyList(RewriteNode(nextNode, nodes ::: rewrittenChildren), nextTail))
        }
    }

    stackSafeRewrite(NonEmptyList.one(RewriteChildren(tree)))
  }

}
