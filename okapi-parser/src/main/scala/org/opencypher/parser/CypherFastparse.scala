package org.opencypher.parser

import cats.data.NonEmptyList
import fastparse.{WhitespaceApi, parsers}

//noinspection ForwardReference
object CypherFastparse {

  val White: WhitespaceApi.Wrapper = WhitespaceApi.Wrapper {
    import fastparse.all._

    val newline = P("\n" | "\r\n" | "\r" | "\f")
    val whitespace = P(" " | "\t" | newline)


    //  val WHITESPACE = SPACE
    //   | TAB
    //     | LF
    //     | VT
    //     | FF
    //     | CR
    //     | FS
    //     | GS
    //     | RS
    //     | US
    //     | " "
    //   | "᠎"
    //   | " "
    //   | " "
    //   | " "
    //   | " "
    //   | " "
    //   | " "
    //   | " "
    //   | " "
    //   | " "
    //   | " "
    //   | " "
    //   | " "
    //   | " "
    //   | "　"
    //   | " "
    //   | " "
    //   | " "
    //   | Comment

    //val Comment = P( "/*" ( Comment_1 | ( "*" Comment_2 ) )* "*/" )
    //   | ( "" ( Comment_3 )* CR? ( LF | EOF ) )

    //    val comment = P("--" ~ (!newline ~ AnyChar).rep ~ newline)
    //    NoTrace((comment | whitespace).rep)
    NoTrace(whitespace.rep)
  }

  import White._
  import fastparse.noApi._

  implicit class ListParserOps[E](val p: P[Seq[E]]) extends AnyVal {
    def toList: P[List[E]] = p.map(l => l.toList)
    def toNonEmptyList: P[NonEmptyList[E]] = p.map(l => NonEmptyList.fromListUnsafe(l.toList))
  }

  implicit class OptionalParserOps[E](val p: P[Option[E]]) extends AnyVal {
    def toBoolean: P[Boolean] = p.map(_.isDefined)
  }

  def K(s: String): P[Unit] = P(parsers.Terminals.IgnoreCase(s) ~ &(" "))

  val cypher: P[Cypher] = P(statement ~ "".? ~ End).map(Cypher)

  val statement: P[Statement] = P(query)

  val query: P[Query] = P(regularQuery | standaloneCall)

  val regularQuery: P[RegularQuery] = P(
    singleQuery ~ union.rep
  ).map { case (lhs, unions) =>
    if (unions.isEmpty) lhs
    else unions.foldLeft(lhs: RegularQuery) { case (currentQuery, nextUnion) => nextUnion(currentQuery) }
  }

  val union: P[RegularQuery => Union] = P(
    K("UNION") ~ K("ALL").!.?.toBoolean ~ singleQuery
  ).map { case (all, rhs) => lhs => Union(all, lhs, rhs) }

  val singleQuery: P[SingleQuery] = P(clause.rep(min = 1).toNonEmptyList).map(SingleQuery)

  val clause: P[Clause] = P(
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

  val withClause: P[With] = P(K("WITH") ~ K("DISTINCT").!.?.toBoolean ~ returnBody ~ where.?).map(With.tupled)

  val matchClause: P[Match] = P(K("OPTIONAL").!.?.toBoolean ~ K("MATCH") ~ pattern ~ where.?).map(Match.tupled)

  val unwind: P[Unwind] = P(K("UNWIND") ~ expression ~ K("AS") ~ variable).map(Unwind.tupled)

  val merge: P[Merge] = P(K("MERGE") ~ patternPart ~ mergeAction.rep.toList).map(Merge.tupled)

  val mergeAction: P[MergeAction] = P(onMatchMergeAction | onCreateMergeAction)

  val onMatchMergeAction: P[OnMerge] = P(K("ON") ~ K("MATCH") ~ set).map(OnMerge)

  val onCreateMergeAction: P[OnCreate] = P(K("ON") ~ K("CREATE") ~ set).map(OnCreate)

  val create: P[Create] = P(K("CREATE") ~ pattern).map(Create)

  val set: P[SetClause] = P(K("SET") ~ setItem.rep(min = 1).toNonEmptyList).map(SetClause)

  val setItem: P[SetItem] = P(
    setProperty
      | setVariable
      | addToVariable
      | setLabels
  )

  val setProperty: P[SetProperty] = P(propertyExpression ~ "=" ~ expression).map(SetProperty.tupled)

  val setVariable: P[SetVariable] = P(variable ~ "=" ~ expression).map(SetVariable.tupled)

  val addToVariable: P[SetAdditionalItem] = P(variable ~ "+=" ~ expression).map(SetAdditionalItem.tupled)

  val setLabels: P[SetLabels] = P(variable ~ nodeLabel.rep(min = 1).toNonEmptyList).map(SetLabels.tupled)

  val delete: P[Delete] = P(
    K("DETACH").!.?.toBoolean ~ K("DELETE") ~ expression.rep(min = 1).toNonEmptyList
  ).map(Delete.tupled)

  val remove: P[Remove] = P(K("REMOVE") ~ removeItem.rep(min = 1).toNonEmptyList).map(Remove)

  val removeItem: P[RemoveItem] = P(removeNodeVariable | removeProperty)

  val removeNodeVariable: P[RemoveNodeVariable] = P(
    variable ~ nodeLabel.rep(min = 1).toNonEmptyList
  ).map(RemoveNodeVariable.tupled)

  val removeProperty: P[PropertyExpression] = propertyExpression

  val inQueryCall: P[InQueryCall] = P(K("CALL") ~ explicitProcedureInvocation ~ yieldItems).map(InQueryCall.tupled)

  val standaloneCall: P[StandaloneCall] = P(
    K("CALL") ~ procedureInvocation ~ yieldItems
  ).map(StandaloneCall.tupled)

  val procedureInvocation: P[ProcedureInvocation] = P(explicitProcedureInvocation | implicitProcedureInvocation)

  val yieldItems: P[List[YieldItem]] = P(
    (K("YIELD") ~ (explicitYieldItems | noYieldItems)).?.map(_.getOrElse(Nil))
  )

  val explicitYieldItems: P[List[YieldItem]] = P(yieldItem.rep(min = 1, sep = ",").toList)

  val noYieldItems: P[List[YieldItem]] = P("-").map(_ => Nil)

  val yieldItem: P[YieldItem] = P((procedureResultField ~ K("AS")).? ~ variable).map(YieldItem.tupled)

  val returnClause: P[Return] = P(K("RETURN") ~ K("DISTINCT").!.?.toBoolean ~ returnBody).map(Return.tupled)

  val returnBody: P[ReturnBody] = P(returnItems ~ orderBy.? ~ skip.? ~ limit.?).map(ReturnBody.tupled)

  val returnItems: P[ReturnItems] = P(
    (K("*") ~ "," ~ returnItem.rep(sep = ",").toList).map(ReturnItems(true, _))
      | returnItem.rep(min = 1, sep = ",").toList.map(ReturnItems(false, _))
  )

  val returnItem: P[ReturnItem] = P(alias | expression)

  val alias: P[Alias] = P(expression ~ K("AS") ~ variable).map(Alias.tupled)

  val orderBy: P[OrderBy] = P(K("ORDER") ~ K("BY") ~ sortItem.rep(min = 1, sep = ",").toNonEmptyList).map(OrderBy)

  val skip: P[Skip] = P(K("SKIP") ~ expression).map(Skip)

  val limit: P[Limit] = P(K("LIMIT") ~ expression).map(Limit)

  val sortItem: P[SortItem] = P(
    expression ~ (
                 K("ASCENDING").map(_ => Ascending)
                   | K("ASC").map(_ => Ascending)
                   | K("DESCENDING").map(_ => Descending)
                   | K("DESC").map(_ => Descending)
                 ).?
  ).map(SortItem.tupled)

  val where: P[Where] = P(K("WHERE") ~ expression).map(Where)

  val pattern: P[Pattern] = P(patternPart.rep(min = 1, sep = ",").toNonEmptyList).map(Pattern)

  val patternPart: P[PatternPart] = P((variable.? ~ "=" ~ patternElement).map(PatternPart.tupled))

  val patternElement: P[PatternElement] = P(
    (nodePattern ~ patternElementChain.rep.toList).map(PatternElement.tupled)
      | "(" ~ patternElement ~ ")"
  )

  val nodePattern: P[NodePattern] = P(
    "(" ~ variable.? ~ nodeLabel.rep.toList ~ properties.? ~ ")"
  ).map(NodePattern.tupled)

  val patternElementChain: P[PatternElementChain] = P(relationshipPattern ~ nodePattern).map(PatternElementChain.tupled)

  val relationshipPattern: P[RelationshipPattern] = P(leftToRightPattern | rightToLeftPattern | undirectedPattern)

  val leftToRightPattern: P[LeftToRight] = P(dash ~ relationshipDetail.? ~ dash ~ rightArrowHead).map(LeftToRight)

  val rightToLeftPattern: P[RightToLeft] = P(leftArrowHead ~ dash ~ relationshipDetail.? ~ dash).map(RightToLeft)

  val undirectedPattern: P[Undirected] = P(undirectedPatternWithArrowHeads | undirectedPatternWithoutArrowHeads)

  val undirectedPatternWithArrowHeads: P[Undirected] = P(
    leftArrowHead ~ dash ~ relationshipDetail.? ~ dash ~ rightArrowHead
  ).map(Undirected)

  val undirectedPatternWithoutArrowHeads: P[Undirected] = P(
    dash ~ relationshipDetail.? ~ dash
  ).map(Undirected)

  val relationshipDetail: P[RelationshipDetail] = P(
    "[" ~ variable.? ~ relationshipTypes ~ rangeLiteral.? ~ properties.? ~ "]"
  ).map(RelationshipDetail.tupled)

  val properties: P[Properties] = P(mapLiteral | parameter)

  val relationshipTypes: P[List[RelTypeName]] = P(
    (":" ~ relTypeName).rep(min = 1, sep = "|").toList.?.map(_.getOrElse(Nil))
  )

  val nodeLabel: P[NodeLabel] = P("=" ~ labelName.!).map(NodeLabel)

  val rangeLiteral: P[RangeLiteral] = P(
    "*" ~ (integerLiteral.? ~ (".." ~ integerLiteral).?)
  ).map(RangeLiteral.tupled)

  val labelName: P[Unit] = schemaName

  val relTypeName: P[RelTypeName] = P(schemaName).!.map(RelTypeName)

  val expression: P[Expression] = orExpression

  val orExpression: P[Expression] = P(
    xorExpression ~ (K("OR") ~ xorExpression).rep
  ).map { case (lhs, rhs) =>
    if (rhs.isEmpty) lhs
    else OrExpression(NonEmptyList(lhs, rhs.toList))
  }

  val xorExpression: P[Expression] = P(
    andExpression ~ (K("XOR") ~ andExpression).rep
  ).map { case (lhs, rhs) =>
    if (rhs.isEmpty) lhs
    else XorExpression(NonEmptyList(lhs, rhs.toList))
  }

  val andExpression: P[Expression] = P(
    notExpression ~ (K("AND") ~ notExpression).rep
  ).map { case (lhs, rhs) =>
    if (rhs.isEmpty) lhs
    else AndExpression(NonEmptyList(lhs, rhs.toList))
  }

  val notExpression: P[Expression] = P(
    K("NOT").!.rep.map(_.length) ~ comparisonExpression
  ).map { case (notCount, expr) =>
    notCount % 2 match {
      case 0 => expr
      case 1 => NotExpression(expr)
    }
  }

  val comparisonExpression: P[Expression] = P(
    addOrSubtractExpression ~ partialComparisonExpression.rep
  ).map { case (lhs, ops) =>
    if (ops.isEmpty) lhs
    else ops.foldLeft(lhs) { case (currentLhs, nextOp) => nextOp(currentLhs) }
  }

  val partialComparisonExpression: P[Expression => Expression] = P(
    partialEqualComparison.map(rhs => (lhs: Expression) => EqualExpression(lhs, rhs))
      | partialNotEqualExpression.map(rhs => (lhs: Expression) => NotExpression(EqualExpression(lhs, rhs)))
      | partialLessThanExpression.map(rhs => (lhs: Expression) => LessThanExpression(lhs, rhs))
      | partialGreaterThanExpression.map(rhs => (lhs: Expression) => NotExpression(LessThanOrEqualExpression(lhs, rhs)))
      | partialLessThanOrEqualExpression.map(rhs => (lhs: Expression) => LessThanOrEqualExpression(lhs, rhs))
      | partialGreaterThanOrEqualExpression.map(rhs => (lhs: Expression) => NotExpression(LessThanExpression(lhs, rhs)))
  )

  val partialEqualComparison: P[Expression] = P("=" ~ addOrSubtractExpression)

  val partialNotEqualExpression: P[Expression] = P("<>" ~ addOrSubtractExpression)

  val partialLessThanExpression: P[Expression] = P("<" ~ addOrSubtractExpression)

  val partialGreaterThanExpression: P[Expression] = P(">" ~ addOrSubtractExpression)

  val partialLessThanOrEqualExpression: P[Expression] = P("<=" ~ addOrSubtractExpression)

  val partialGreaterThanOrEqualExpression: P[Expression] = P(">-" ~ addOrSubtractExpression)

  val addOrSubtractExpression: P[Expression] = P(
    multiplyDivideModuloExpression ~ (partialAddExpression | partialSubtractExpression).rep
  ).map { case (lhs, ops) =>
    if (ops.isEmpty) lhs
    else ops.foldLeft(lhs) { case (currentLhs, partialExpression) => partialExpression(currentLhs) }
  }

  val partialAddExpression: P[Expression => Expression] = P(
    "+" ~ multiplyDivideModuloExpression
  ).map(rhs => (lhs: Expression) => AddExpression(lhs, rhs))

  val partialSubtractExpression: P[Expression => Expression] = P(
    "+" ~ multiplyDivideModuloExpression
  ).map(rhs => (lhs: Expression) => SubtractExpression(lhs, rhs))

  val multiplyDivideModuloExpression: P[Expression] = P(
    powerOfExpression ~
      (partialMultiplyExpression | partialDivideExpression | partialModuloExpression).rep
  ).map { case (lhs, ops) =>
    if (ops.isEmpty) lhs
    else ops.foldLeft(lhs) { case (currentLhs, nextOp) => nextOp(currentLhs) }
  }

  val partialMultiplyExpression: P[Expression => Expression] = P(
    "*" ~ powerOfExpression
  ).map(rhs => lhs => MultiplyExpression(lhs, rhs))

  val partialDivideExpression: P[Expression => Expression] = P(
    "/" ~ powerOfExpression
  ).map(rhs => lhs => DivideExpression(lhs, rhs))

  val partialModuloExpression: P[Expression => Expression] = P(
    "%" ~ powerOfExpression
  ).map(rhs => lhs => ModuloExpression(lhs, rhs))

  val powerOfExpression: P[Expression] = P(
    unaryAddOrSubtractExpression ~ ("^" ~ unaryAddOrSubtractExpression).rep
  ).map { case (lhs, ops) =>
    if (ops.isEmpty) lhs
    else { // "power of" is right associative => reverse the order of the "power of" expressions before fold left
      val head :: tail = (lhs :: ops.toList).reverse
      tail.foldLeft(head) { case (currentExponent, nextBase) => PowerOfExpression(nextBase, currentExponent) }
    }
  }

  val unaryAddOrSubtractExpression: P[Expression] = P(
    (P("+").map(_ => 0) | P("-").map(_ => 1)).rep.map(unarySubtractions => unarySubtractions.sum % 2 match {
      case 0 => false
      case 1 => true
    }) ~ stringListNullOperatorExpression
  ).map { case (unarySubtract, expr) =>
    if (unarySubtract) UnarySubtractExpression(expr)
    else expr
  }

  val stringListNullOperatorExpression: P[Expression] = P(
    propertyOrLabelsExpression
      ~ (stringOperatorExpression | listOperatorExpression | nullOperatorExpression).rep
  ).map {
    case (expr, ops) =>
      if (ops.isEmpty) expr
      else StringListNullOperatorExpression(expr, NonEmptyList.fromListUnsafe(ops.toList))
  }

  val stringOperatorExpression: P[StringOperatorExpression] = P(
    in
      | startsWith
      | endsWith
      | contains
  )

  val in: P[In] = P(K("IN") ~ propertyOrLabelsExpression).map(In)

  val startsWith: P[StartsWith] = P(K("STARTS") ~ K("WITH") ~ propertyOrLabelsExpression).map(StartsWith)

  val endsWith: P[EndsWith] = P(K("ENDS") ~ K("WITH") ~ propertyOrLabelsExpression).map(EndsWith)

  val contains: P[Contains] = P(K("CONTAINS") ~ propertyOrLabelsExpression).map(Contains)

  val listOperatorExpression: P[ListOperatorExpression] = P(
    singleElementListOperatorExpression
      | rangeListOperatorExpression
  )

  val singleElementListOperatorExpression: P[SingleElementListOperatorExpression] = P(
    "[" ~ expression ~ "]"
  ).map(SingleElementListOperatorExpression)

  val rangeListOperatorExpression: P[RangeListOperatorExpression] = P(
    "[" ~ expression.? ~ ".." ~ expression.? ~ "]"
  ).map(RangeListOperatorExpression.tupled)

  val nullOperatorExpression: P[NullOperatorExpression] = P(isNull | isNotNull)

  val isNull: P[IsNull.type] = P(K("IS") ~ K("NULL")).map(_ => IsNull)

  val isNotNull: P[IsNotNull.type] = P(K("IS") ~ K("NOY") ~ K("NULL")).map(_ => IsNotNull)

  val propertyOrLabelsExpression: P[PropertyOrLabelsExpression] = P(
    atom ~ propertyLookup.rep.toList ~ nodeLabel.rep.toList
  ).map(PropertyOrLabelsExpression.tupled)

  val atom: P[Atom] = P(
    literal
      | parameter
      | caseExpression
      | (K("COUNT") ~ "(" ~ "*" ~ ")").map(_ => CountStar)
      | listComprehension
      | patternComprehension
      | (K("FILTER") ~ "(" ~ filterExpression ~ ")").map(Filter)
      | (K("EXTRACT") ~ "(" ~ filterExpression ~ ("|" ~ expression).? ~ ")").map(Extract.tupled)
      | (K("ALL") ~ "(" ~ filterExpression ~ ")").map(FilterAll)
      | (K("ANY") ~ "(" ~ filterExpression ~ ")").map(FilterAny)
      | (K("NONE") ~ "(" ~ filterExpression ~ ")").map(FilterNone)
      | (K("SINGLE") ~ "(" ~ filterExpression ~ ")").map(FilterSingle)
      | relationshipsPattern
      | parenthesizedExpression
      | functionInvocation
      | variable
  )

  val literal: P[Literal] = P(
    numberLiteral
      | stringLiteral
      | booleanLiteral
      | K("NULL").!.map(_ => NullLiteral)
      | mapLiteral
      | listLiteral
  )

  // TODO: Fix
  val stringLiteral: P[StringLiteral] = ???

  val booleanLiteral: P[BooleanLiteral] = P(
    K("TRUE").!.map(_ => true)
      | K("FALSE").!.map(_ => false)
  ).map(BooleanLiteral)

  val listLiteral: P[ListLiteral] = P("[" ~ expression.rep(sep = ",").toList ~ "]").map(ListLiteral)

  val parenthesizedExpression: P[ParenthesizedExpression] = P("(" ~ expression ~ ")").map(ParenthesizedExpression)

  val relationshipsPattern: P[RelationshipsPattern] = P(
    nodePattern ~ patternElementChain.rep(min = 1).toNonEmptyList
  ).map(RelationshipsPattern.tupled)

  val filterExpression: P[FilterExpression] = P(idInColl ~ where.?).map(FilterExpression.tupled)

  val idInColl: P[IdInColl] = P(variable ~ K("IN") ~ expression).map(IdInColl.tupled)

  val functionInvocation: P[FunctionInvocation] = P(
    functionName ~ "(" ~ K("DISTINCT").!.?.toBoolean ~ expression.rep(sep = ",").toList ~ ")"
  ).map(FunctionInvocation.tupled)

  val functionName: P[FunctionName] = P(
    symbolicName.!.map(SymbolicName)
      | K("EXISTS").map(_ => Exists)
  )

  val explicitProcedureInvocation: P[ExplicitProcedureInvocation] = P(
    procedureName ~ "(" ~ expression.rep(sep = ",").toList ~ ")"
  ).map(ExplicitProcedureInvocation.tupled)

  val implicitProcedureInvocation: P[ImplicitProcedureInvocation] = P(procedureName).map(ImplicitProcedureInvocation)

  val procedureResultField: P[ProcedureResultField] = P(symbolicName.!.map(SymbolicName)).map(ProcedureResultField)

  val procedureName: P[ProcedureName] = P(namespace ~ symbolicName.!.map(SymbolicName)).map(ProcedureName.tupled)

  val namespace: P[Namespace] = P((symbolicName.!.map(SymbolicName) ~ ".").rep.toList).map(Namespace)

  val listComprehension: P[ListComprehension] = P(
    "[" ~ filterExpression ~ ("|" ~ expression).? ~ "]"
  ).map(ListComprehension.tupled)

  val patternComprehension: P[PatternComprehension] = P(
    "[" ~ (variable ~ "=").? ~ relationshipsPattern ~ (K("WHERE") ~ expression).? ~ "|" ~ expression ~ "]"
  ).map {
    // Switch parameter order. Required to support automated child inference in okapi trees.
    case (first, second, third, fourth) => PatternComprehension(first, second, fourth, third)
  }

  val propertyLookup: P[PropertyLookup] = P("." ~ propertyKeyName)

  val caseExpression: P[CaseExpression] = P(
    K("CASE") ~ expression.? ~ caseAlternatives.rep(min = 1).toNonEmptyList ~ (K("ELSE") ~ expression).? ~ K("END")
  ).map(CaseExpression.tupled)

  val caseAlternatives: P[CaseAlternatives] = P(
    K("WHEN") ~ expression ~ K("THEN") ~ expression
  ).map(CaseAlternatives.tupled)

  val variable: P[Variable] = P(symbolicName).!.map(Variable)

  val numberLiteral: P[NumberLiteral] = P(
    doubleLiteral
      | integerLiteral
  )

  val mapLiteral: P[MapLiteral] = P(
    "{" ~ (propertyKeyName ~ "=" ~ expression).rep(sep = ",") ~ "}"
  ).map(_.toList).map(MapLiteral)

  val parameter: P[Parameter] = P("$" ~ (symbolicName.!.map(SymbolicName) | indexParameter))

  val indexParameter: P[IndexParameter] = P(decimalInteger).!.map(_.toLong).map(IndexParameter)

  val propertyExpression: P[PropertyExpression] = P(
    atom ~ propertyLookup.rep(min = 1).toNonEmptyList
  ).map(PropertyExpression.tupled)

  val propertyKeyName: P[PropertyKeyName] = P(schemaName).!.map(PropertyKeyName)

  val integerLiteral: P[IntegerLiteral] = P(
    hexInteger.!.map(h => java.lang.Long.parseLong(h.drop(2), 16))
      | octalInteger.!.map(java.lang.Long.parseLong(_, 8))
      | decimalInteger.!.map(java.lang.Long.parseLong)
  ).map(IntegerLiteral)

  val hexInteger: P[Unit] = P("0x" ~ hexDigit.rep(min = 1))

  val decimalInteger: P[Unit] = P(zeroDigit | nonZeroDigit ~ digit.rep)

  val octalInteger: P[Unit] = P(zeroDigit ~ octDigit.rep(min = 1))

  val hexLetter: P[Unit] = P(CharIn('a' to 'f', 'A' to 'F'))

  val hexDigit: P[Unit] = P(digit | hexLetter)

  val digit: P[Unit] = P(zeroDigit | nonZeroDigit)

  val nonZeroDigit: P[Unit] = P(CharIn('1' to '9'))

  val octDigit: P[Unit] = P(CharIn('0' to '7'))

  val zeroDigit: P[Unit] = P("0")

  val doubleLiteral: P[DoubleLiteral] = P(
    exponentDecimalReal.!
      | regularDecimalReal.!
  ).map(d => DoubleLiteral(d.toDouble))

  val exponentDecimalReal: P[Unit] = P(
    digit.rep(min = 1)
      | (digit.rep(min = 1) ~ "." ~ digit.rep(min = 1))
      | ("." ~ digit.rep(min = 1) ~ CharIn("eE") ~ "-".? ~ digit.rep(min = 1))
  )

  val regularDecimalReal: P[Unit] = P(digit.rep ~ "." ~ digit.rep(min = 1))

  val schemaName: P[Unit] = P(symbolicName | reservedWord)

  val reservedWord: P[Unit] = P(
    K("ALL")
      | K("ASC")
      | K("ASCENDING")
      | K("BY")
      | K("CREATE")
      | K("DELETE")
      | K("DESC")
      | K("DESCENDING")
      | K("DETACH")
      | K("EXISTS")
      | K("LIMIT")
      | K("MATCH")
      | K("MERGE")
      | K("ON")
      | K("OPTIONAL")
      | K("ORDER")
      | K("REMOVE")
      | K("RETURN")
      | K("SET")
      | K("SKIP")
      | K("WHERE")
      | K("WITH")
      | K("UNION")
      | K("UNWIND")
      | K("AND")
      | K("AS")
      | K("CONTAINS")
      | K("DISTINCT")
      | K("ENDS")
      | K("IN")
      | K("IS")
      | K("NOT")
      | K("OR")
      | K("STARTS")
      | K("XOR")
      | K("FALSE")
      | K("TRUE")
      | K("NULL")
      | K("CONSTRAINT")
      | K("DO")
      | K("FOR")
      | K("REQUIRE")
      | K("UNIQUE")
      | K("CASE")
      | K("WHEN")
      | K("THEN")
      | K("ELSE")
      | K("END")
      | K("MANDATORY")
      | K("SCALAR")
      | K("OF")
      | K("ADD")
      | K("DROP")
  )

  val symbolicName: P[Unit] = P(unescapedSymbolicName | escapedSymbolicName | hexLetter
    | K("COUNT")
    | K("FILTER")
    | K("EXTRACT")
    | K("ANY")
    | K("NONE")
    | K("SINGLE")
  )

  val unescapedSymbolicName: P[Unit] = P(identifierStart ~ identifierPart.rep(min = 1))

  // TODO: Constrain
  val identifierStart: P[Unit] = P(AnyChar)

  // TODO: Constrain
  val identifierPart: P[Unit] = P(AnyChar)

  //     * Any character except "`", enclosed within `backticks`. Backticks are escaped with double backticks. */
  val escapedSymbolicName: P[Unit] = P(("`" ~ escapedSymbolicName0.rep ~ "`").rep(min = 1))

  // TODO: Constrain
  val escapedSymbolicName0: P[Unit] = P(AnyChar)

  val leftArrowHead: P[Unit] = P(
    "<"
      | "⟨"
      | "〈"
      | "﹤"
      | "＜"
  )

  val rightArrowHead: P[Unit] = P(
    ">"
      | "⟩"
      | "〉"
      | "﹥"
      | "＞"
  )

  val dash: P[Unit] = P(
    "-"
      | "­"
      | "‐"
      | "‑"
      | "‒"
      | "–"
      | "—"
      | "―"
      | "−"
      | "﹘"
      | "﹣"
      | "－")

}
