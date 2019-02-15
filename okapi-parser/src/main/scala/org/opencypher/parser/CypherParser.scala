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
package org.opencypher.parser

import java.lang.Character.UnicodeBlock

import cats.data.NonEmptyList
import fastparse.Parsed.{Failure, Success}
import org.opencypher.okapi.api.exception.CypherException
import org.opencypher.okapi.api.exception.CypherException.ErrorPhase.CompileTime
import org.opencypher.okapi.api.exception.CypherException.ErrorType.SyntaxError
import org.opencypher.okapi.api.exception.CypherException._
import org.opencypher.okapi.api.value.CypherValue.CypherValue
import fastparse._
import org.opencypher.parser.CypherAst._
import org.opencypher.parser.CypherExpressions._

import scala.annotation.tailrec

case class ParsingException(override val detail: ErrorDetails)
  extends CypherException(SyntaxError, CompileTime, detail)

object Whitespace {
  final def newline[_: P]: P[Unit] = P("\n" | "\r\n" | "\r" | "\f")
  final def invisible[_: P]: P[Unit] = P(" " | "\t" | newline)
  final def comment[_: P]: P[Unit] = P("--" ~ (!newline ~ AnyChar).rep ~ newline)

  implicit val cypherWhitespace: P[_] => P[Unit] = { implicit ctx: ParsingRun[_] => (comment | invisible).repX(0) }
}

object CypherParser {

  import Whitespace.cypherWhitespace

  final def parseCypher9(query: String, parameters: Map[String, CypherValue] = Map.empty): CypherTree = {
    val parseResult = parse(query, cypherQueryEntireInput(_), verboseFailures = true)
    val ast = parseResult match {
      case Success(v, _) => v
      case Failure(expected, index, extra) =>
        val i = extra.input
        val before = index - math.max(index - 20, 0)
        val after = math.min(index + 20, i.length) - index
        println(extra.input.slice(index - before, index + after).replace('\n', ' '))
        println("~" * before + "^" + "~" * after)

        val maybeNextCharacter = if (index < i.length) Some(i(index)) else None
        maybeNextCharacter match {
          case Some(c) if UnicodeBlock.of(c) != UnicodeBlock.BASIC_LATIN =>
            throw ParsingException(InvalidUnicodeCharacter(s"'$c' is not a valid unicode character"))
          case _ =>
        }

        // TODO: Parse without end instead and check end index?
        val parsedQueryBeforeFailure: Option[CypherTree] = {
          parse(i, cypherQuery(_), verboseFailures = true) match {
            case Success(v, _) => Some(v)
            case _ => None
          }
        }

        parsedQueryBeforeFailure match {
          case Some(s: StandaloneCall) =>
            throw ParsingException(InvalidArgumentPassingMode(
              s"\n`${s.procedureInvocation.procedureName}` needs explicit arguments, unless it is used in a standalone call."))
          case _ =>
        }

        val lastSuccessfulParse: Option[CypherTree] = {
          @tailrec def lastChild(ast: CypherTree): CypherTree = {
            if (ast.children.length == 0) ast else lastChild(ast.children.last)
          }

          parsedQueryBeforeFailure.map(lastChild)
        }

        lastSuccessfulParse match {
          case Some(_: NumberLiteral) if maybeNextCharacter.isDefined =>
            throw ParsingException(InvalidNumberLiteral(
              s"\n'${maybeNextCharacter.get}' is not a valid next character for a number literal."))
          case _ =>
        }

        val traced = extra.trace()
        traced.stack.last match {
          case (parser, _) if parser == "relationshipDetail" && maybeNextCharacter.isDefined =>
            throw ParsingException(InvalidRelationshipPattern(
              s"'${maybeNextCharacter.get}' is not a valid part of a relationship pattern"))
          case other => println(s"Last stack frame: $other")
        }

        println(s"Expected=$expected")
        println(s"Message=${traced.msg}")
        println(s"Aggregate message=${traced.aggregateMsg}")
        println(s"Stack=${traced.stack}")
        throw new Exception(expected)
    }
    ast
  }

  implicit class ContainerParserOps[E](val p: P[Seq[E]]) extends AnyVal {
    final def toSet: P[Set[E]] = p.map(_.toSet)
    final def toList: P[List[E]] = p.map(_.toList)
    final def toNonEmptyList: P[NonEmptyList[E]] = p.map(l => NonEmptyList.fromListUnsafe(l.toList))
  }

  implicit class OptionalParserOps[E](val p: P[Option[E]]) extends AnyVal {
    final def toBoolean: P[Boolean] = p.map(_.isDefined)
  }

  final def K(s: String)(implicit ctx: P[Any]): P[Unit] = IgnoreCase(s) ~~ &(CharIn(" \t\n\r\f") | End)

  final def cypherQueryEntireInput[_: P]: P[Query] = P(Start ~ cypherQuery ~ ";".? ~ End)

  final def cypherQuery[_: P]: P[Query] = P(regularQuery | standaloneCall)

  final def regularQuery[_: P]: P[RegularQuery] = P(
    singleQuery ~ union.rep
  ).map { case (lhs, unions) =>
    if (unions.isEmpty) lhs
    else unions.foldLeft(lhs: RegularQuery) { case (currentQuery, nextUnion) => nextUnion(currentQuery) }
  }

  final def union[_: P]: P[RegularQuery => Union] = P(
    K("UNION") ~ K("ALL").!.?.toBoolean ~ singleQuery
  ).map { case (all, rhs) => lhs => Union(all, lhs, rhs) }

  final def singleQuery[_: P]: P[SingleQuery] = P(clause.rep(1).toNonEmptyList).map(SingleQuery)

  final def clause[_: P]: P[Clause] = P(
    merge
      | delete
      | set
      | create
      | remove
      | withClause
      | matchClause
      | unwind
      | inQueryCall
      | returnClause
  )

  final def withClause[_: P]: P[With] = P(K("WITH") ~/ K("DISTINCT").!.?.toBoolean ~/ returnBody ~ where.?).map(With.tupled)

  final def matchClause[_: P]: P[Match] = P(K("OPTIONAL").!.?.toBoolean ~ K("MATCH") ~/ pattern ~ where.?).map(Match.tupled)

  final def unwind[_: P]: P[Unwind] = P(K("UNWIND") ~/ expression ~ K("AS") ~/ variable).map(Unwind.tupled)

  final def merge[_: P]: P[Merge] = P(K("MERGE") ~/ patternPart ~ mergeAction.rep.toList).map(Merge.tupled)

  final def mergeAction[_: P]: P[MergeAction] = P(K("ON") ~/ (onCreateMergeAction | onMatchMergeAction))

  final def onMatchMergeAction[_: P]: P[OnMerge] = P(K("MATCH") ~/ set).map(OnMerge)

  final def onCreateMergeAction[_: P]: P[OnCreate] = P(K("CREATE") ~/ set).map(OnCreate)

  final def create[_: P]: P[Create] = P(IgnoreCase("CREATE") ~/ pattern).map(Create)

  final def set[_: P]: P[SetClause] = P(K("SET") ~/ setItem.rep(1).toNonEmptyList).map(SetClause)

  final def setItem[_: P]: P[SetItem] = P(
    setProperty
      | setVariable
      | addToVariable
      | setLabels
  )

  final def setProperty[_: P]: P[SetProperty] = P(propertyExpression ~ "=" ~ expression).map(SetProperty.tupled)

  final def setVariable[_: P]: P[SetVariable] = P(variable ~ "=" ~ expression).map(SetVariable.tupled)

  final def addToVariable[_: P]: P[SetAdditionalItem] = P(variable ~ "+=" ~ expression).map(SetAdditionalItem.tupled)

  final def setLabels[_: P]: P[SetLabels] = P(variable ~ nodeLabel.rep(1).toNonEmptyList).map(SetLabels.tupled)

  final def delete[_: P]: P[Delete] = P(
    K("DETACH").!.?.toBoolean ~ K("DELETE") ~/ expression.rep(1, ",").toNonEmptyList
  ).map(Delete.tupled)

  final def remove[_: P]: P[Remove] = P(K("REMOVE") ~/ removeItem.rep(1, ",").toNonEmptyList).map(Remove)

  final def removeItem[_: P]: P[RemoveItem] = P(removeNodeVariable | removeProperty)

  final def removeNodeVariable[_: P]: P[RemoveNodeVariable] = P(
    variable ~ nodeLabel.rep(1).toNonEmptyList
  ).map(RemoveNodeVariable.tupled)

  final def removeProperty[_: P]: P[Property] = P(propertyExpression)

  final def inQueryCall[_: P]: P[InQueryCall] = P(K("CALL") ~ explicitProcedureInvocation ~ yieldItems).map(InQueryCall.tupled)

  final def standaloneCall[_: P]: P[StandaloneCall] = P(
    K("CALL") ~ procedureInvocation ~ yieldItems
  ).map(StandaloneCall.tupled)

  final def procedureInvocation[_: P]: P[ProcedureInvocation] = P(explicitProcedureInvocation | implicitProcedureInvocation)

  final def yieldItems[_: P]: P[List[YieldItem]] = P(
    (K("YIELD") ~ (explicitYieldItems | noYieldItems)).?.map(_.getOrElse(Nil))
  )

  final def explicitYieldItems[_: P]: P[List[YieldItem]] = P(yieldItem.rep(1, ",").toList)

  final def noYieldItems[_: P]: P[List[YieldItem]] = P("-").map(_ => Nil)

  final def yieldItem[_: P]: P[YieldItem] = P((procedureResultField ~ K("AS")).? ~ variable).map(YieldItem.tupled)

  final def returnClause[_: P]: P[Return] = P(K("RETURN") ~/ K("DISTINCT").!.?.toBoolean ~/ returnBody).map(Return.tupled)

  final def returnBody[_: P]: P[ReturnBody] = P(returnItems ~ orderBy.? ~ skip.? ~ limit.?).map(ReturnBody.tupled)

  final def returnItems[_: P]: P[ReturnItems] = P(
    ("*" ~/ ("," ~ returnItem.rep(1, ",").toList).?
      .map(_.getOrElse(Nil))).map(ReturnItems(true, _))
      | returnItem.rep(1, ",").toList.map(ReturnItems(false, _))
  )

  final def returnItem[_: P]: P[ReturnItem] = P(
    expression ~ alias.?
  ).map {
    case (e, None) => e
    case (e, Some(a)) => a(e)
  }

  final def alias[_: P]: P[Expression => Alias] = P(K("AS") ~ variable).map(v => e => Alias(e, v))

  final def orderBy[_: P]: P[OrderBy] = P(K("ORDER BY") ~/ sortItem.rep(1, ",").toNonEmptyList).map(OrderBy)

  final def skip[_: P]: P[Skip] = P(K("SKIP") ~ expression).map(Skip)

  final def limit[_: P]: P[Limit] = P(K("LIMIT") ~ expression).map(Limit)

  final def sortItem[_: P]: P[SortItem] = P(
    expression ~ (
                 IgnoreCase("ASCENDING").map(_ => Ascending)
                   | IgnoreCase("ASC").map(_ => Ascending)
                   | IgnoreCase("DESCENDING").map(_ => Descending)
                   | IgnoreCase("DESC").map(_ => Descending)
                 ).?
  ).map(SortItem.tupled)

  final def where[_: P]: P[Where] = P(K("WHERE") ~ expression).map(Where)

  final def pattern[_: P]: P[Pattern] = P(patternPart.rep(1, ",").toNonEmptyList).map(Pattern)

  final def patternPart[_: P]: P[PatternPart] = P((variable ~ "=").? ~ patternElement).map(PatternPart.tupled)

  final def patternElement[_: P]: P[PatternElement] = P(
    (nodePattern ~ patternElementChain.rep.toList).map(PatternElement.tupled)
      | "(" ~ patternElement ~ ")"
  )

  final def nodePattern[_: P]: P[NodePattern] = P(
    "(" ~ variable.? ~ nodeLabel.rep.toSet ~ properties.? ~ ")"
  ).map(NodePattern.tupled)

  final def patternElementChain[_: P]: P[PatternElementChain] = P(relationshipPattern ~ nodePattern).map(PatternElementChain.tupled)

  final def relationshipPattern[_: P]: P[RelationshipPattern] = P(
    hasLeftArrow ~/ relationshipDetail ~ hasRightArrow
  ).map {
    case (false, detail, true) => LeftToRight(detail)
    case (true, detail, false) => RightToLeft(detail)
    case (_, detail, _) => Undirected(detail)
  }

  final def hasLeftArrow[_: P]: P[Boolean] = P(leftArrowHead.!.?.map(_.isDefined) ~ dash)

  final def hasRightArrow[_: P]: P[Boolean] = P(dash ~ rightArrowHead.!.?.map(_.isDefined))

  final def relationshipDetail[_: P]: P[RelationshipDetail] = P(
    "[" ~/ variable.? ~/ relationshipTypes ~/ rangeLiteral.? ~/ properties.? ~/ "]"
  ).?.map(_.map(RelationshipDetail.tupled).getOrElse(RelationshipDetail(None, Set.empty, None, None)))

  final def properties[_: P]: P[Properties] = P(mapLiteral | parameter)

  final def relationshipTypes[_: P]: P[Set[String]] = P(
    (":" ~ relTypeName.rep(1, "|" ~ ":".?)).toSet.?.map(_.getOrElse(Set.empty))
  )

  final def nodeLabel[_: P]: P[String] = P(":" ~ labelName)

  final def rangeLiteral[_: P]: P[RangeLiteral] = P(
    "*" ~/ integerLiteral.? ~ (".." ~ integerLiteral.?).?.map(_.flatten)
  ).map(RangeLiteral.tupled)

  final def labelName[_: P]: P[String] = P(schemaName.!)

  final def relTypeName[_: P]: P[String] = P(schemaName.!)

  final def expression[_: P]: P[Expression] = P(orExpression)

  final def orExpression[_: P]: P[Expression] = P(
    xorExpression ~ (K("OR") ~ xorExpression).rep
  ).map { case (lhs, rhs) =>
    if (rhs.isEmpty) lhs
    else Or(NonEmptyList(lhs, rhs.toList))
  }

  final def xorExpression[_: P]: P[Expression] = P(
    andExpression ~ (K("XOR") ~ andExpression).rep
  ).map { case (lhs, rhs) =>
    if (rhs.isEmpty) lhs
    else Xor(NonEmptyList(lhs, rhs.toList))
  }

  final def andExpression[_: P]: P[Expression] = P(
    notExpression ~ (K("AND") ~ notExpression).rep
  ).map { case (lhs, rhs) =>
    if (rhs.isEmpty) lhs
    else And(NonEmptyList(lhs, rhs.toList))
  }

  final def notExpression[_: P]: P[Expression] = P(
    K("NOT").!.rep.map(_.length) ~ comparisonExpression
  ).map { case (notCount, expr) =>
    notCount % 2 match {
      case 0 => expr
      case 1 => Not(expr)
    }
  }

  final def comparisonExpression[_: P]: P[Expression] = P(
    addOrSubtractExpression ~ partialComparisonExpression.rep
  ).map { case (lhs, ops) =>
    if (ops.isEmpty) lhs
    else ops.foldLeft(lhs) { case (currentLhs, nextOp) => nextOp(currentLhs) }
  }

  final def partialComparisonExpression[_: P]: P[Expression => Expression] = P(
    partialEqualComparison.map(rhs => (lhs: Expression) => Equal(lhs, rhs))
      | partialNotEqualExpression.map(rhs => (lhs: Expression) => Not(Equal(lhs, rhs)))
      | partialLessThanExpression.map(rhs => (lhs: Expression) => LessThan(lhs, rhs))
      | partialGreaterThanExpression.map(rhs => (lhs: Expression) => Not(LessThanOrEqual(lhs, rhs)))
      | partialLessThanOrEqualExpression.map(rhs => (lhs: Expression) => LessThanOrEqual(lhs, rhs))
      | partialGreaterThanOrEqualExpression.map(rhs => (lhs: Expression) => Not(LessThan(lhs, rhs)))
  )

  final def partialEqualComparison[_: P]: P[Expression] = P("=" ~ addOrSubtractExpression)

  final def partialNotEqualExpression[_: P]: P[Expression] = P("<>" ~ addOrSubtractExpression)

  final def partialLessThanExpression[_: P]: P[Expression] = P("<" ~ addOrSubtractExpression)

  final def partialGreaterThanExpression[_: P]: P[Expression] = P(">" ~ addOrSubtractExpression)

  final def partialLessThanOrEqualExpression[_: P]: P[Expression] = P("<=" ~ addOrSubtractExpression)

  final def partialGreaterThanOrEqualExpression[_: P]: P[Expression] = P(">=" ~ addOrSubtractExpression)

  final def addOrSubtractExpression[_: P]: P[Expression] = P(
    multiplyDivideModuloExpression ~ (partialAddExpression | partialSubtractExpression).rep
  ).map { case (lhs, ops) =>
    if (ops.isEmpty) lhs
    else ops.foldLeft(lhs) { case (currentLhs, partialExpression) => partialExpression(currentLhs) }
  }

  final def partialAddExpression[_: P]: P[Expression => Expression] = P(
    "+" ~ multiplyDivideModuloExpression
  ).map(rhs => (lhs: Expression) => Add(lhs, rhs))

  final def partialSubtractExpression[_: P]: P[Expression => Expression] = P(
    "-" ~ multiplyDivideModuloExpression
  ).map(rhs => (lhs: Expression) => Subtract(lhs, rhs))

  final def multiplyDivideModuloExpression[_: P]: P[Expression] = P(
    powerOfExpression ~
      (partialMultiplyExpression | partialDivideExpression | partialModuloExpression).rep
  ).map { case (lhs, ops) =>
    if (ops.isEmpty) lhs
    else ops.foldLeft(lhs) { case (currentLhs, nextOp) => nextOp(currentLhs) }
  }

  final def partialMultiplyExpression[_: P]: P[Expression => Expression] = P(
    "*" ~ powerOfExpression
  ).map(rhs => lhs => Multiply(lhs, rhs))

  final def partialDivideExpression[_: P]: P[Expression => Expression] = P(
    "/" ~ powerOfExpression
  ).map(rhs => lhs => Divide(lhs, rhs))

  final def partialModuloExpression[_: P]: P[Expression => Expression] = P(
    "%" ~ powerOfExpression
  ).map(rhs => lhs => Modulo(lhs, rhs))

  final def powerOfExpression[_: P]: P[Expression] = P(
    unaryAddOrSubtractExpression ~ ("^" ~ unaryAddOrSubtractExpression).rep
  ).map { case (lhs, ops) =>
    if (ops.isEmpty) lhs
    else { // "power of" is right associative => reverse the order of the "power of" expressions before fold left
      val head :: tail = (lhs :: ops.toList).reverse
      tail.foldLeft(head) { case (currentExponent, nextBase) => PowerOf(nextBase, currentExponent) }
    }
  }

  final def unaryAddOrSubtractExpression[_: P]: P[Expression] = P(
    (P("+").map(_ => 0) | P("-").map(_ => 1)).rep.map(unarySubtractions => unarySubtractions.sum % 2 match {
      case 0 => false
      case 1 => true
    }) ~ stringListNullOperatorExpression
  ).map { case (unarySubtract, expr) =>
    if (unarySubtract) UnarySubtract(expr)
    else expr
  }

  final def stringListNullOperatorExpression[_: P]: P[Expression] = P(
    propertyOrLabelsExpression
      ~ (stringOperatorExpression | listOperatorExpression | nullOperatorExpression).rep
  ).map {
    case (expr, ops) =>
      if (ops.isEmpty) expr
      else StringListNullOperator(expr, NonEmptyList.fromListUnsafe(ops.toList))
  }

  final def stringOperatorExpression[_: P]: P[StringOperator] = P(
    startsWith
      | endsWith
      | contains
  )

  final def in[_: P]: P[In] = P(K("IN") ~ propertyOrLabelsExpression).map(In)

  final def startsWith[_: P]: P[StartsWith] = P(K("STARTS WITH") ~ propertyOrLabelsExpression).map(StartsWith)

  final def endsWith[_: P]: P[EndsWith] = P(K("ENDS WITH") ~ propertyOrLabelsExpression).map(EndsWith)

  final def contains[_: P]: P[Contains] = P(K("CONTAINS") ~ propertyOrLabelsExpression).map(Contains)

  final def listOperatorExpression[_: P]: P[ListOperator] = P(
    in
      | singleElementListOperatorExpression
      | rangeListOperatorExpression
  )

  final def singleElementListOperatorExpression[_: P]: P[SingleElementListOperator] = P(
    "[" ~ expression ~ "]"
  ).map(SingleElementListOperator)

  final def rangeListOperatorExpression[_: P]: P[RangeListOperator] = P(
    "[" ~ expression.? ~ ".." ~ expression.? ~ "]"
  ).map(RangeListOperator.tupled)

  final def nullOperatorExpression[_: P]: P[NullOperator] = P(K("IS") ~/ K("NOT").!.?.toBoolean ~/ K("NULL")).map {
    case true => IsNotNull
    case false => IsNull
  }

  final def propertyOrLabelsExpression[_: P]: P[Expression] = P(
    atom ~ propertyLookup.rep.toList ~ nodeLabel.rep.toList
  ).map { case (a, pls, nls) =>
    if (pls.isEmpty && nls.isEmpty) a
    else PropertyOrLabels(a, pls, nls)
  }

  final def atom[_: P]: P[Atom] = P(
    literal
      | parameter
      | caseExpression
      | patternComprehension
      | listComprehension
      | (IgnoreCase("COUNT") ~ "(" ~ "*" ~ ")").map(_ => CountStar)
      | (IgnoreCase("ALL") ~ "(" ~ filterExpression ~ ")").map(FilterAll)
      | (IgnoreCase("ANY") ~ "(" ~ filterExpression ~ ")").map(FilterAny)
      | (IgnoreCase("NONE") ~ "(" ~ filterExpression ~ ")").map(FilterNone)
      | (IgnoreCase("SINGLE") ~ "(" ~ filterExpression ~ ")").map(FilterSingle)
      | relationshipsPattern
      | parenthesizedExpression
      | functionInvocation
      | variable
  )

  final def literal[_: P]: P[Literal] = P(
    numberLiteral
      | stringLiteral
      | booleanLiteral
      | IgnoreCase("NULL").map(_ => NullLiteral)
      | mapLiteral
      | listLiteral
  )

  def stringLiteral[_: P]: P[StringLiteral] = P(stringLiteralDoubleQuotationMarkDelimited | stringLiteralSingleQuoteMarkDelimited)

  def newline[_: P]: P[Unit] = P("\n" | "\r\n" | "\r" | "\f")

  def escapedChar[_: P]: P[String] = P("\\" ~~ !(newline | hexDigit) ~~ AnyChar.!)

  // TODO: Resolve escaped characters
  def stringLiteralWithDelimiter(delimiter: Char)(implicit ctx: P[Any]): P[StringLiteral] =
    P(CharPred(_ == delimiter) ~~ (escapedChar | CharsWhile(c => c != delimiter && c != '\\')).repX.! ~~ CharPred(_ == delimiter)).map(StringLiteral)

  def stringLiteralDoubleQuotationMarkDelimited[_: P]: P[StringLiteral] = stringLiteralWithDelimiter('\"')

  def stringLiteralSingleQuoteMarkDelimited[_: P]: P[StringLiteral] = stringLiteralWithDelimiter('\'')

  final def booleanLiteral[_: P]: P[BooleanLiteral] = P(
    IgnoreCase("TRUE").map(_ => true)
      | IgnoreCase("FALSE").map(_ => false)
  ).map(BooleanLiteral)

  final def listLiteral[_: P]: P[ListLiteral] = P("[" ~ NoCut(expression).rep(0, ",").toList ~ "]").map(ListLiteral)

  final def parenthesizedExpression[_: P]: P[ParenthesizedExpression] = P("(" ~ expression ~ ")").map(ParenthesizedExpression)

  final def relationshipsPattern[_: P]: P[RelationshipsPattern] = P(
    nodePattern ~ patternElementChain.rep(1).toNonEmptyList
  ).map(RelationshipsPattern.tupled)

  final def filterExpression[_: P]: P[Filter] = P(idInColl ~ where.?).map(Filter.tupled)

  final def idInColl[_: P]: P[IdInColl] = P(variable ~ K("IN") ~ expression).map(IdInColl.tupled)

  final def functionInvocation[_: P]: P[FunctionInvocation] = P(
    functionName ~ "(" ~ K("DISTINCT").!.?.toBoolean ~ expression.rep(0, ",").toList ~ ")"
  ).map(FunctionInvocation.tupled)

  final def functionName[_: P]: P[FunctionName] = P(
    (namespace ~~ symbolicName.!)
      | K("EXISTS").map(_ => Nil -> "EXISTS")
  ).map(FunctionName.tupled)

  final def explicitProcedureInvocation[_: P]: P[ExplicitProcedureInvocation] = P(
    procedureName ~ "(" ~ expression.rep(0, ",").toList ~ ")"
  ).map(ExplicitProcedureInvocation.tupled)

  final def implicitProcedureInvocation[_: P]: P[ImplicitProcedureInvocation] = P(procedureName).map(ImplicitProcedureInvocation)

  final def procedureResultField[_: P]: P[String] = P(symbolicName.!)

  final def procedureName[_: P]: P[ProcedureName] = P(namespace ~ symbolicName.!).map(ProcedureName.tupled)

  final def namespace[_: P]: P[List[String]] = P((symbolicName.! ~ ".").rep.toList)

  final def listComprehension[_: P]: P[ListComprehension] = P(
    "[" ~ filterExpression ~ ("|" ~ expression).? ~ "]"
  ).map(ListComprehension.tupled)

  final def patternComprehension[_: P]: P[PatternComprehension] = P(
    "[" ~ (variable ~ "=").? ~ NoCut(relationshipsPattern) ~ (K("WHERE") ~ expression).? ~ "|" ~ expression ~ "]"
  ).map {
    // Switch parameter order. Required to support automated child inference in okapi trees.
    case (first, second, third, fourth) => PatternComprehension(first, second, fourth, third)
  }

  final def propertyLookup[_: P]: P[String] = P("." ~ propertyKeyName)

  final def caseExpression[_: P]: P[CaseExpression] = P(
    K("CASE") ~ expression.? ~ caseAlternatives.rep(1).toNonEmptyList ~ (K("ELSE") ~ expression).? ~ K("END")
  ).map(CaseExpression.tupled)

  final def caseAlternatives[_: P]: P[CaseAlternatives] = P(
    K("WHEN") ~ expression ~ K("THEN") ~ expression
  ).map(CaseAlternatives.tupled)

  final def variable[_: P]: P[Variable] = P(symbolicName).!.map(Variable)

  final def numberLiteral[_: P]: P[NumberLiteral] = P(
    doubleLiteral
      | integerLiteral
  )

  final def propertyLiteral[_: P]: P[PropertyLiteral] = P(propertyKeyName ~ ":" ~ expression).map(PropertyLiteral.tupled)

  final def mapLiteral[_: P]: P[MapLiteral] = P(
    "{" ~/ propertyLiteral.rep(0, ",") ~/ "}"
  ).map(_.toList).map(MapLiteral)

  final def parameter[_: P]: P[Parameter] = P("$" ~ (symbolicName.!.map(ParameterName) | indexParameter))

  final def indexParameter[_: P]: P[IndexParameter] = P(decimalInteger).!.map(_.toLong).map(IndexParameter)

  final def propertyExpression[_: P]: P[Property] = P(
    atom ~ propertyLookup.rep(1).toNonEmptyList
  ).map(Property.tupled)

  final def propertyKeyName[_: P]: P[String] = P(schemaName.!)

  final def integerLiteral[_: P]: P[IntegerLiteral] = P(
    hexInteger.!.map(h => java.lang.Long.parseLong(h.drop(2), 16))
      | octalInteger.!.map(java.lang.Long.parseLong(_, 8))
      | decimalInteger.!.map(java.lang.Long.parseLong)
  ).map(IntegerLiteral)

  final def hexInteger[_: P]: P[Unit] = P("0x" ~ hexDigit.rep(1))

  final def decimalInteger[_: P]: P[Unit] = P(zeroDigit | (nonZeroDigit ~ digit.rep))

  final def octalInteger[_: P]: P[Unit] = P(zeroDigit ~ octDigit.rep(1))

  final def hexDigit[_: P]: P[Unit] = P(CharIn("0-9a-fA-F"))

  final def digit[_: P]: P[Unit] = P(CharIn("0-9"))

  final def nonZeroDigit[_: P]: P[Unit] = P(CharIn("1-9"))

  final def octDigit[_: P]: P[Unit] = P(CharIn("0-7"))

  final def zeroDigit[_: P]: P[Unit] = P("0")

  // TODO: Simplify
  final def doubleLiteral[_: P]: P[DoubleLiteral] = P(
    exponentDecimalReal.!
      | regularDecimalReal.!
  ).map { d =>
    val parsed = d.toDouble
    if (parsed == Double.PositiveInfinity || parsed == Double.NegativeInfinity) {
      throw ParsingException(FloatingPointOverflow(s"'$d' is too large to represent as a Java Double"))
    } else {
      DoubleLiteral(parsed)
    }
  }

  final def exponentDecimalReal[_: P]: P[Unit] = P(
    ((digit.repX(1) ~~ "." ~~ digit.repX(1))
      | ("." ~~ digit.repX(1))
      | digit.rep(1)
    ) ~~ CharIn("eE") ~~ "-".? ~~ digit.repX(1)
  )

  final def regularDecimalReal[_: P]: P[Unit] = P(digit.rep ~ "." ~ digit.rep(1))

  final def schemaName[_: P]: P[Unit] = P(symbolicName)

  final def symbolicName[_: P]: P[Unit] = P(unescapedSymbolicName | escapedSymbolicName)

  final def unescapedSymbolicName[_: P]: P[Unit] = P(identifierPart.repX(1))

  // TODO: Expand
  final def identifierStart[_: P]: P[Unit] = P(CharIn("a-zA-Z"))

  // TODO: Expand
  final def identifierPart[_: P]: P[Unit] = P(CharIn("a-zA-Z0-9_"))

  // TODO: Resolve escaped characters
  // Any character except "`", enclosed within `backticks`. Backticks are escaped with double backticks. */
  final def escapedSymbolicName[_: P]: P[Unit] = P(("`" ~~ (CharsWhile(_ != '`') | "``").repX ~~ "`").repX(1))

  // TODO: Expand
  final def leftArrowHead[_: P]: P[Unit] = P("<")

  // TODO: Expand
  final def rightArrowHead[_: P]: P[Unit] = P(">")

  // TODO: Expand
  final def dash[_: P]: P[Unit] = P("-")

}
