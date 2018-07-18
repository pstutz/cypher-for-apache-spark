package org.opencypher.okapi.trees

import cats.data.NonEmptyList

import scala.annotation.tailrec
import scala.reflect.ClassTag

trait RewriterStackSafe[I <: TreeNode[I], O] extends TreeRewriter[I, O] {

  type Stack = NonEmptyList[TreeOperation[I, O]]

}

sealed trait TreeOperation[T <: TreeNode[T], O]

case class RewriteChildren[I <: TreeNode[I], O](node: I, rewrittenChildren: List[O] = List.empty[O]) extends TreeOperation[I, O]

case class RewriteNode[I <: TreeNode[I], O](node: I, rewrittenChildren: List[O] = List.empty[O]) extends TreeOperation[I, O]

case class Done[I <: TreeNode[I], O](nodes: List[O]) extends TreeOperation[I, O]

case class BottomUpStackSafe[T <: TreeNode[T] : ClassTag](rule: PartialFunction[T, T]) extends RewriterStackSafe[T, T] {

  @inline
  final def updateNode(node: T): T = if (rule.isDefinedAt(node)) rule(node) else node

  override def rewrite(tree: T): T = {

    @tailrec
    def stackSafeRewrite(stack: Stack): T = stack match {
      case NonEmptyList(RewriteChildren(node, rewrittenChildren), tail) =>
        val updatedStack = if (node.children.isEmpty) {
          NonEmptyList(Done[T, T](updateNode(node) :: rewrittenChildren), tail)
        } else {
          node.children.foldLeft(NonEmptyList(RewriteNode(node, rewrittenChildren), tail)) {
            case (currentStack, child) =>
              RewriteChildren(child, List.empty[T]) :: currentStack
          }
        }
        stackSafeRewrite(updatedStack)
      case NonEmptyList(RewriteNode(node, rewrittenChildren), tail) =>
        val (currentRewrittenChildren, nextRewrittenChildren) = rewrittenChildren.splitAt(node.children.length)
        val nodeWithUpdatedChildren = node.withNewChildren(currentRewrittenChildren.toArray)
        val updatedStack = NonEmptyList(Done[T, T](updateNode(nodeWithUpdatedChildren) :: nextRewrittenChildren), tail)
        stackSafeRewrite(updatedStack)
      case NonEmptyList(Done(nodes), tail) =>
        tail match {
          case Nil => nodes match {
              case result :: Nil => result
              case invalid => throw new IllegalStateException(
                s"Invalid rewrite produced $invalid instead of a single final rewritten tree root.")
            }
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

case class TopDownStackSafe[T <: TreeNode[T] : ClassTag](rule: PartialFunction[T, T]) extends RewriterStackSafe[T, T] {

  @inline
  final def updateNode(node: T): T = if (rule.isDefinedAt(node)) rule(node) else node

  override def rewrite(tree: T): T = {

    @tailrec
    def stackSafeRewrite(stack: Stack): T = stack match {
      case NonEmptyList(RewriteChildren(node, rewrittenChildren), tail) =>
        val updatedStack = if (node.children.isEmpty) {
          NonEmptyList(Done[T, T](updateNode(node) :: rewrittenChildren), tail)
        } else {
          val updatedNode = updateNode(node)
          updatedNode.children.foldLeft(NonEmptyList(RewriteNode(updatedNode, rewrittenChildren), tail)) {
            case (currentStack, child) =>
              RewriteChildren(child, List.empty[T]) :: currentStack
          }
        }
        stackSafeRewrite(updatedStack)
      case NonEmptyList(RewriteNode(node, rewrittenChildren), tail) =>
        val (currentRewrittenChildren, nextRewrittenChildren) = rewrittenChildren.splitAt(node.children.length)
        val nodeWithUpdatedChildren = node.withNewChildren(currentRewrittenChildren.toArray)
        val updatedStack = NonEmptyList(Done[T, T](nodeWithUpdatedChildren :: nextRewrittenChildren), tail)
        stackSafeRewrite(updatedStack)
      case NonEmptyList(Done(nodes), tail) =>
        tail match {
          case Nil => nodes match {
              case result :: Nil => result
              case invalid => throw new IllegalStateException(
                s"Invalid rewrite produced $invalid instead of a single final rewritten tree root.")
            }
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

case class TransformBottomUpStackSafe[I <: TreeNode[I] : ClassTag, O](transform: (I, List[O]) => O)
  extends TreeRewriter[I, O] {

  type Stack = NonEmptyList[TreeOperation[I, O]]

  override def rewrite(tree: I): O = {

    @tailrec
    def stackSafeRewrite(stack: Stack): O = stack match {
      case NonEmptyList(RewriteChildren(node, rewrittenChildren), tail) =>
        val updatedStack = if (node.children.isEmpty) {
          NonEmptyList(Done[I, O](transform(node, List.empty[O]) :: rewrittenChildren), tail)
        } else {
          node.children.foldLeft(NonEmptyList(RewriteNode(node, rewrittenChildren), tail)) {
            case (currentStack, child) =>
              RewriteChildren(child, List.empty[O]) :: currentStack
          }
        }
        stackSafeRewrite(updatedStack)
      case NonEmptyList(RewriteNode(node, transformedChildren), tail) =>
        val (currentRewrittenChildren, nextRewrittenChildren) = transformedChildren.splitAt(node.children.length)
        val transformedNode = transform(node, currentRewrittenChildren)
        val updatedStack = NonEmptyList(Done[I, O](transformedNode :: nextRewrittenChildren), tail)
        stackSafeRewrite(updatedStack)
      case NonEmptyList(Done(nodes), tail) =>
        tail match {
          case Nil => nodes match {
              case result :: Nil => result
              case invalid => throw new IllegalStateException(
                s"Invalid rewrite produced $invalid instead of a single final rewritten tree root.")
            }
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

case class TransformTopDownStackSafe[I <: TreeNode[I] : ClassTag, O](
  transform: I => O,
  aggregate: (O, List[O]) => O
) extends TreeRewriter[I, O] {

  type Stack = NonEmptyList[TreeOperation[I, O]]

  override def rewrite(tree: I): O = {

    @tailrec
    def stackSafeRewrite(stack: Stack): O = stack match {
      case NonEmptyList(RewriteChildren(node, transformedChildren), tail) =>
        val updatedStack = if (node.children.isEmpty) {
          NonEmptyList(Done[I, O](aggregate(transform(node), List.empty[O]) :: transformedChildren), tail)
        } else {
          node.children.foldLeft(NonEmptyList(RewriteNode(node, transformedChildren), tail)) {
            case (currentStack, child) =>
              RewriteChildren(child, List.empty[O]) :: currentStack
          }
        }
        stackSafeRewrite(updatedStack)
      case NonEmptyList(RewriteNode(node, transformedChildren), tail) =>
        val (currentTransformedChildren, nextTransformedChildren) = transformedChildren.splitAt(node.children.length)
        val transformedNode = aggregate(transform(node), currentTransformedChildren)
        val updatedStack = NonEmptyList(Done[I, O](transformedNode :: nextTransformedChildren), tail)
        stackSafeRewrite(updatedStack)
      case NonEmptyList(Done(nodes), tail) =>
        tail match {
          case Nil => nodes match {
            case result :: Nil => result
            case invalid => throw new IllegalStateException(
              s"Invalid rewrite produced $invalid instead of a single final rewritten tree root.")
          }
          case Done(nextNodes) :: nextTail =>
            stackSafeRewrite(NonEmptyList(Done(nodes ::: nextNodes), nextTail))
          case RewriteChildren(nextNode, transformedChildren) :: nextTail =>
            stackSafeRewrite(NonEmptyList(RewriteChildren(nextNode, nodes ::: transformedChildren), nextTail))
          case RewriteNode(nextNode, transformedChildren) :: nextTail =>
            stackSafeRewrite(NonEmptyList(RewriteNode(nextNode, nodes ::: transformedChildren), nextTail))
        }
    }

    stackSafeRewrite(NonEmptyList.one(RewriteChildren(tree)))
  }

}
