/*
 * Copyright (c) 2016-2019 "Neo4j Sweden, AB" [https://neo4j.com]
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
package org.opencypher.okapi.ir.impl.parse.rewriter.legacy

import org.neo4j.cypher.internal.v4_0.ast._
import org.neo4j.cypher.internal.v4_0.expressions.{Expression, LogicalVariable}
import org.neo4j.cypher.internal.v4_0.util._

/**
  * This rewriter normalizes the scoping structure of a query, ensuring it is able to
  * be correctly processed for semantic checking. It makes sure that all return items
  * in a WITH clauses are aliased.
  *
  * It also replaces expressions and subexpressions in ORDER BY and WHERE
  * to use aliases introduced by the WITH, where possible.
  *
  * This rewriter depends on normalizeReturnClauses having first been run.
  *
  * Example:
  *
  * MATCH n
  * WITH n.prop AS prop ORDER BY n.prop DESC
  * RETURN prop
  *
  * This rewrite will change the query to:
  *
  * MATCH n
  * WITH n.prop AS prop ORDER BY prop DESC
  * RETURN prop
  */
case class normalizeWithClauses(mkException: (String, InputPosition) => CypherException) extends Rewriter {

  def apply(that: AnyRef): AnyRef = instance.apply(that)

  private val clauseRewriter: (Clause => Clause) = {
    // Only alias return items
    case clause@With(_, ri: ReturnItems, None, _, _, None) =>
      val (unaliasedReturnItems, aliasedReturnItems) = partitionReturnItems(ri.items)
      val initialReturnItems = unaliasedReturnItems ++ aliasedReturnItems
      clause.copy(returnItems = ri.copy(items = initialReturnItems)(ri.position))(clause.position)

    // Alias return items and rewrite ORDER BY and WHERE
    case clause@With(distinct, ri: ReturnItems, orderBy, skip, limit, where) =>
      clause.verifyOrderByAggregationUse((s, i) => throw mkException(s, i))
      val (unaliasedReturnItems, aliasedReturnItems) = partitionReturnItems(ri.items)
      val initialReturnItems = unaliasedReturnItems ++ aliasedReturnItems

      val existingAliases = aliasedReturnItems.map(i => i.expression -> i.alias.get.copyId).toMap
      val updatedOrderBy = orderBy.map(aliasOrderBy(existingAliases, _))
      val updatedWhere = where.map(aliasWhere(existingAliases, _))

      clause.copy(returnItems = ri.copy(items = initialReturnItems)(ri.position), orderBy = updatedOrderBy, where = updatedWhere)(clause.position)

    // Not our business
    case clause =>
      clause
  }

  /**
    * Aliases return items if possible. Return a tuple of unaliased (because impossible) and
    * aliased (because they already were aliases or we just introduced an alias for them)
    * return items.
    */
  private def partitionReturnItems(returnItems: Seq[ReturnItem]): (Seq[ReturnItem], Seq[AliasedReturnItem]) =
    returnItems.foldLeft((Vector.empty[ReturnItem], Vector.empty[AliasedReturnItem])) {
      case ((unaliasedItems, aliasedItems), item) => item match {
        case i: AliasedReturnItem =>
          (unaliasedItems, aliasedItems :+ i)

        case i if i.alias.isDefined =>
          (unaliasedItems, aliasedItems :+ AliasedReturnItem(item.expression, item.alias.get.copyId)(item.position))

        case _ =>
          // Unaliased return items in WITH will be preserved so that semantic check can report them as an error
          (unaliasedItems :+ item, aliasedItems)
      }
    }

  /**
    * Given a list of existing aliases, this rewrites an OrderBy to use these where possible.
    */
  private def aliasOrderBy(existingAliases: Map[Expression, LogicalVariable], originalOrderBy: OrderBy): OrderBy = {
    val updatedSortItems = originalOrderBy.sortItems.map { aliasSortItem(existingAliases, _)}
    OrderBy(updatedSortItems)(originalOrderBy.position)
  }

  /**
    * Given a list of existing aliases, this rewrites a SortItem to use these where possible.
    */
  private def aliasSortItem(existingAliases: Map[Expression, LogicalVariable], sortItem: SortItem): SortItem = {
    sortItem match {
      case AscSortItem(expression) => AscSortItem(aliasExpression(existingAliases, expression))(sortItem.position)
      case DescSortItem(expression) => DescSortItem(aliasExpression(existingAliases, expression))(sortItem.position)
    }
  }

  /**
    * Given a list of existing aliases, this rewrites a where to use these where possible.
    */
  private def aliasWhere(existingAliases: Map[Expression, LogicalVariable], originalWhere: Where): Where = {
    Where(aliasExpression(existingAliases, originalWhere.expression))(originalWhere.position)
  }

  /**
    * Given a list of existing aliases, this rewrites expressions to use these where possible.
    */
  private def aliasExpression(existingAliases: Map[Expression, LogicalVariable], expression: Expression): Expression = {
    existingAliases.get(expression) match {
      case Some(alias) =>
        alias.copyId

      case None =>
        val newExpression = expression.endoRewrite(topDown(Rewriter.lift {
          case subExpression: Expression =>
            existingAliases.get(subExpression).map(_.copyId).getOrElse(subExpression)
        }))
        newExpression
    }
  }

  private val instance: Rewriter = bottomUp(Rewriter.lift {
    case query@SingleQuery(clauses) =>
      query.copy(clauses = clauses.map(clauseRewriter))(query.position)
  })
}
