package org.opencypher.parser

import fastparse.{WhitespaceApi, parsers}

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

  object | {
    def apply[I](i: I): I = i
  }

  def K(s: String): P[Unit] = P(parsers.Terminals.IgnoreCase(s) ~ &(" "))

  val cypher = P(statement ~ "".? ~ End)

  val statement = P(query)

  val query = P(regularQuery | standaloneCall)

  val regularQuery = P(singleQuery ~ union.rep)

  val union = P(K("UNION") ~ K("ALL").? ~ singleQuery)

  val singleQuery = P(clause.rep(min = 1))

  val clause = P(
    merge
      | delete
      | set
      | create
      | remove
      | withClause
      | matchClause
      | unwind
      | inQueryCall
      | returnClause)

  val withClause = P(K("WITH") ~ K("DISTINCT").?.! ~ returnBody ~ where.?)

  val matchClause = P(K("OPTIONAL").?.! ~ K("MATCH") ~ pattern ~ where.?)

  val unwind = P(K("UNWIND") ~ expression ~ K("AS") ~ variable)

  val merge = P(K("MERGE") ~ patternPart ~ mergeAction.rep)

  val mergeAction = P(onMatchMergeAction | onCreateMergeAction)

  val onMatchMergeAction = P(K("ON") ~ K("MATCH") ~ set)

  val onCreateMergeAction = P(K("ON") ~ K("CREATE") ~ set)

  val create = P(K("CREATE") ~ pattern)

  val set = P(K("SET") ~ setItem.rep(min = 1))

  val setItem = P(
    setProperty
      | setVariable
      | addToVariable
      | setLabels
  )

  val setProperty = P(propertyExpression ~ "=" ~ expression)

  val setVariable = P(variable ~ "=" ~ expression)

  val addToVariable = P(variable ~ "+=" ~ expression)

  val setLabels = P(variable ~ nodeLabels)

  val delete = P(K("DETACH").?.! ~ K("DELETE") ~ expression.rep(min = 1))

  val remove = P(K("REMOVE") ~ removeItem.rep(min = 1))

  val removeItem = P(removeLabel | removeProperty)

  val removeLabel = P(variable ~ nodeLabels)

  val removeProperty = P(propertyExpression)

  val inQueryCall = P(K("CALL") ~ explicitProcedureInvocation ~ (K("YIELD") ~ yieldItems).?)

  val standaloneCall = P(K("CALL") ~ (explicitProcedureInvocation | implicitProcedureInvocation) ~ (K("YIELD") ~ yieldItems).?)

  val yieldItems = P(explicitYieldItems | allYieldItems)

  val explicitYieldItems = P(yieldItem.rep(min = 1, sep = ","))

  val allYieldItems = P("-")

  val yieldItem = P((procedureResultField ~ K("AS")).? ~ variable)

  val returnClause = P(K("RETURN") ~ K("DISTINCT").?.! ~ returnBody)

  val returnBody = P(returnItems ~ order.? ~ skip.? ~ limit.?)

  // TODO: Validate either star or nonempty explicit return items
  val returnItems = P(K("*").? ~ ("," ~ returnItem).rep.?)

  val returnItem = P(aliasedReturnItem | simpleReturnItem)

  val aliasedReturnItem = P(expression ~ K("AS") ~ variable)

  val simpleReturnItem = P(expression)

  val order = P(K("ORDER") ~ K("BY") ~ sortItem.rep(min = 1, sep = ","))

  val skip = P(K("SKIP") ~ expression)

  val limit = P(K("LIMIT") ~ expression)

  val sortItem = P(expression ~ (K("ASCENDING") | K("ASC") | K("DESCENDING") | K("DESC")).?)

  val where = P(K("WHERE") ~ expression)

  val pattern = P(patternPart.rep(min = 1, sep = ","))

  val patternPart = P(namedPatternPart | anonymousPatternPart)

  val namedPatternPart = P(variable ~ "=" ~ anonymousPatternPart)

  val anonymousPatternPart = P(patternElement)

  val patternElement: P[PatternElement] = P(patternElementWithoutParentheses | patternElementWithParentheses).map(???)

  val patternElementWithoutParentheses = P(nodePattern ~ patternElementChain.rep)

  val patternElementWithParentheses = P("(" ~ patternElement ~ ")")

  val nodePattern = P("(" ~ variable.? ~ nodeLabels.? ~ properties.? ~ ")")

  val patternElementChain = P(relationshipPattern ~ nodePattern)

  val relationshipPattern = P(leftToRightPattern | rightToLeftPattern | undirectedPattern)

  val leftToRightPattern = P(dash ~ relationshipDetail.? ~ dash ~ rightArrowHead)

  val rightToLeftPattern = P(leftArrowHead ~ dash ~ relationshipDetail.? ~ dash)

  val undirectedPattern = P(undirectedPatternWithArrowHeads | undirectedPatternWithoutArrowHeads)

  val undirectedPatternWithArrowHeads = P(leftArrowHead ~ dash ~ relationshipDetail.? ~ dash ~ rightArrowHead)

  val undirectedPatternWithoutArrowHeads = P(dash ~ relationshipDetail.? ~ dash)

  val relationshipDetail = P("[" ~ variable.? ~ relationshipTypes.? ~ rangeLiteral.? ~ properties.? ~ "]")

  val properties = P(mapLiteral | parameter)

  // TODO: First colon is not optional
  val relationshipTypes = P((":" ~ relTypeName).rep(min = 1, sep = "|"))

  val nodeLabels = P(nodeLabel.rep(min = 1))

  val nodeLabel = P("=" ~ labelName)

  val rangeLiteral = P("*" ~ (integerLiteral.? ~ (".." ~ integerLiteral).?))

  val labelName = P(schemaName)

  val relTypeName = P(schemaName)

  val expression: P[Expression] = P(orExpression).map(???)

  val orExpression = P(xorExpression ~ (K("OR") ~ xorExpression).rep)

  val xorExpression = P(andExpression ~ (K("XOR") ~ andExpression).rep)

  val andExpression = P(notExpression ~ (K("AND") ~ notExpression).rep)

  val notExpression = P(K("NOT").rep ~ comparisonExpression)

  val comparisonExpression = P(addOrSubtractExpression ~ partialComparisonExpression.rep)

  val addOrSubtractExpression = P(multiplyDivideModuloExpression
    ~ (("+" ~ multiplyDivideModuloExpression) | ("-" ~ multiplyDivideModuloExpression)).rep)

  val multiplyDivideModuloExpression = P(powerOfExpression
    ~ (("*" ~ powerOfExpression) | ("/" ~ powerOfExpression) | ("%" ~ powerOfExpression)).rep)

  val powerOfExpression = P(unaryAddOrSubtractExpression ~ ("^" ~ unaryAddOrSubtractExpression).rep)

  val unaryAddOrSubtractExpression = P(("+" | "-").rep ~ stringListNullOperatorExpression)

  val stringListNullOperatorExpression = P(propertyOrLabelsExpression
    ~ (stringOperatorExpression | listOperatorExpression | nullOperatorExpression).rep)

  val stringOperatorExpression = P(K("IN") | (K("STARTS") ~ K("WITH")) | (K("ENDS") ~ K("WITH")) | K("CONTAINS")
    ~ propertyOrLabelsExpression)

  val listOperatorExpression = P(singleElementListOperatorExpression | rangeListOperatorExpression)

  val singleElementListOperatorExpression = P("[" ~ expression ~ "]")

  val rangeListOperatorExpression = P("[" ~ expression.? ~ ".." ~ expression.? ~ "]")

  val nullOperatorExpression = P(isNull | isNotNull)

  val isNull = P(K("IS") ~ K("NULL"))

  val isNotNull = P(K("IS") ~ K("NOY") ~ K("NULL"))

  val propertyOrLabelsExpression = P(atom ~ propertyLookup.rep ~ nodeLabels.?)

  val atom: P[Atom] = P(
    literal
      | parameter
      | caseExpression
      | (K("COUNT") ~ "(" ~ "*" ~ ")")
      | listComprehension
      | patternComprehension
      | (K("FILTER") ~ "(" ~ filterExpression ~ ")")
      | (K("EXTRACT") ~ "(" ~ filterExpression ~ ("|" ~ expression).? ~ ")")
      | (K("ALL") ~ "(" ~ filterExpression ~ ")")
      | (K("ANY") ~ "(" ~ filterExpression ~ ")")
      | (K("NONE") ~ "(" ~ filterExpression ~ ")")
      | (K("SINGLE") ~ "(" ~ filterExpression ~ ")")
      | relationshipsPattern
      | parenthesizedExpression
      | functionInvocation
      | variable
  ).map(???)

  val literal = P(
    numberLiteral
      | stringLiteral
      | booleanLiteral
      | K("NULL")
      | mapLiteral
      | listLiteral
  )

  // TODO: Fix
  val stringLiteral: P[StringLiteral] = ???

  val booleanLiteral = P(K("TRUE") | K("FALSE"))

  val listLiteral = P("[" ~ expression.rep(sep = ",") ~ "]")

  val partialComparisonExpression = P(
    ("=" ~ addOrSubtractExpression)
      | ("<>" ~ addOrSubtractExpression)
      | ("<" ~ addOrSubtractExpression)
      | (">" ~ addOrSubtractExpression)
      | ("<=" ~ addOrSubtractExpression)
      | (">=" ~ addOrSubtractExpression)
  )

  val parenthesizedExpression = P("(" ~ expression ~ ")")

  val relationshipsPattern = P(nodePattern ~ patternElementChain.rep(min = 1))

  val filterExpression = P(idInColl ~ where.?)

  val idInColl = P(variable ~ K("IN") ~ expression)

  val functionInvocation = P(functionName ~ "(" ~ K("DISTINCT").?.! ~ expression.rep(sep = ",") ~ ")")

  val functionName = P(symbolicName | K("EXISTS"))

  val explicitProcedureInvocation = P(procedureName ~ "(" ~ expression.rep(sep = ",") ~ ")")

  val implicitProcedureInvocation = P(procedureName)

  val procedureResultField = P(symbolicName)

  val procedureName = P(namespace ~ symbolicName)

  val namespace = P((symbolicName ~ ".").rep)

  val listComprehension = P("[" ~ filterExpression ~ ("|" ~ expression).? ~ "]")

  val patternComprehension = P("[" ~ (variable ~ "=").? ~ relationshipsPattern ~ (K("WHERE") ~ expression).? ~ "|" ~ expression ~ "]")

  val propertyLookup = P("." ~ propertyKeyName)

  val caseExpression = P(
    (K("CASE") ~ caseAlternatives.rep(min = 1))
      | (K("CASE") ~ expression ~ caseAlternatives.rep(min = 1) ~ (K("ELSE") ~ expression).? ~ K("END"))
  )

  val caseAlternatives = P(K("WHEN") ~ expression ~ K("THEN") ~ expression)

  val variable = P(symbolicName)

  val numberLiteral = P(
    doubleLiteral
      | integerLiteral
  )

  val mapLiteral: P[MapLiteral] = P("{" ~ (propertyKeyName ~ "=" ~ expression ~ ("," ~ propertyKeyName ~ "=" ~ expression).rep).? ~ "}").map(???)

  val parameter = P("$" ~ (symbolicName | decimalInteger))

  val propertyExpression = P(atom ~ propertyLookup.rep(min = 1))

  val propertyKeyName = P(schemaName)

  val integerLiteral = P(hexInteger | octalInteger | decimalInteger)

  val hexInteger = P("0x" ~ hexDigit.rep(min = 1))

  val decimalInteger = P(zeroDigit | nonZeroDigit ~ digit.rep)

  val octalInteger = P(zeroDigit ~ octDigit.rep(min = 1))

  val hexLetter = P(CharIn('a' to 'f', 'A' to 'F'))

  val hexDigit = P(digit | hexLetter)

  val digit = P(zeroDigit | nonZeroDigit)

  val nonZeroDigit = P(CharIn('1' to '9'))

  val octDigit = P(CharIn('0' to '7'))

  val zeroDigit = P("0")

  val doubleLiteral = P(
    exponentDecimalReal
      | regularDecimalReal
  )

  val exponentDecimalReal = P(
    digit.rep(min = 1)
      | (digit.rep(min = 1) ~ "." ~ digit.rep(min = 1))
      | ("." ~ digit.rep(min = 1) ~ CharIn("eE") ~ "-".? ~ digit.rep(min = 1))
  )

  val regularDecimalReal = P(digit.rep ~ "." ~ digit.rep(min = 1))

  val schemaName = P(symbolicName | reservedWord)

  val reservedWord = P(
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

  val symbolicName = P(unescapedSymbolicName | escapedSymbolicName | hexLetter
    | K("COUNT")
    | K("FILTER")
    | K("EXTRACT")
    | K("ANY")
    | K("NONE")
    | K("SINGLE")
  )

  val unescapedSymbolicName = P(identifierStart ~ identifierPart.rep(min = 1))

  // TODO: Constrain
  val identifierStart = P(AnyChar)

  // TODO: Constrain
  val identifierPart = P(AnyChar)

  //     * Any character except "`", enclosed within `backticks`. Backticks are escaped with double backticks. */
  val escapedSymbolicName = P(("`" ~ escapedSymbolicName0.rep ~ "`").rep(min = 1))

  // TODO: Constrain
  val escapedSymbolicName0 = P(AnyChar)

  val leftArrowHead = P(
    "<"
      | "⟨"
      | "〈"
      | "﹤"
      | "＜"
  )

  val rightArrowHead = P(
    ">"
      | "⟩"
      | "〉"
      | "﹥"
      | "＞"
  )

  val dash = P(
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
