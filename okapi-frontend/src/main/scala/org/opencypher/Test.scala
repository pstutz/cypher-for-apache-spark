package org.opencypher

import org.antlr.v4.runtime.tree.{ParseTree, RuleNode, TerminalNode}
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream, ParserRuleContext}
import org.opencypher.okapi.trees.AbstractTreeNode
import org.opencypher.parser.CypherParser.OC_MultiplyDivideModuloExpressionContext
import org.opencypher.parser.{CypherBaseVisitor, CypherLexer, CypherParser}
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object Test extends App {

  //  val s = """
  //            |MATCH (n)
  //            |RETURN *, n
  //          """.stripMargin

  val s =
    """
      |RETURN 1 - 1
    """.stripMargin

  val input = CharStreams.fromString(s)
  val lexer = new CypherLexer(input)
  val tokens = new CommonTokenStream(lexer)
  val parser = new CypherParser(tokens)
  val tree = parser.oC_Cypher
  val ast = TestVisitor.visit(tree)
  //ast.show
}

abstract class CypherAst extends AbstractTreeNode[CypherAst]

case object TestVisitor extends CypherBaseVisitor[CypherAst] {

  implicit def terminalToBoolean(t: TerminalNode): Boolean = {
    t != null
  }

  implicit def terminalListToBoolean(t: java.util.List[TerminalNode]): Boolean = {
    t != null && !t.isEmpty
  }

  implicit class RichJavaList[E](val list: java.util.List[E]) extends AnyVal {

    def map[T <: CypherAst](f: E => T): List[T] = {
      list.asScala.toList.map(f(_))
    }

    def terminals(s: String): List[String] = {
      list.asScala.toList.flatMap {
        case t: TerminalNode if s.contains(t.getSymbol.getText) => Some(t.getSymbol.getText)
        case _ => None
      }
    }

    def contains(s: String): Boolean = {
      list.asScala.toList.exists {
        case t: TerminalNode if t.getSymbol.getText == s => true
        case _ => false
      }

    }
  }

  implicit class RichParserContext(val ctx: ParserRuleContext) {
    def contains(s: String): Boolean = {
      ctx.children != null && ctx.children.contains(s)
    }
  }

  def visitEither[P](either: RuleNode): P = {
    var result: P = null.asInstanceOf[P]
    val n = either.getChildCount
    var i = 0
    while (result == null && i < n) {
      val child = either.getChild(i)
      result = child.accept(this).asInstanceOf[P]
      i += 1
    }
    result
  }

  //  def visitList[P](list: List[RuleNode]): List[P] = {
  //    var result = List.empty[P]
  //    val n = list.getChildCount
  //    var i = 0
  //    while (result == null && i < n) {
  //      val child = list.getChild(i)
  //      result ::= child.accept(this).asInstanceOf[P]
  //      i += 1
  //    }
  //    result.reverse
  //  }

  override def visitOC_Cypher(ctx: CypherParser.OC_CypherContext): Cypher = {
    Cypher(visitOC_Statement(ctx.oC_Statement))
  }

  override def visitOC_Statement(ctx: CypherParser.OC_StatementContext): Statement = {
    visitOC_Query(ctx.oC_Query)
  }

  override def visitOC_Query(ctx: CypherParser.OC_QueryContext): Query = {
    visitEither[Query](ctx)
  }

  override def visitOC_RegularQuery(ctx: CypherParser.OC_RegularQueryContext): RegularQuery = {
    RegularQuery(visitOC_SingleQuery(ctx.oC_SingleQuery), ctx.oC_Union.map(visitOC_Union))
  }

  override def visitOC_SingleQuery(ctx: CypherParser.OC_SingleQueryContext): SingleQuery = {
    visitEither[SingleQuery](ctx)
  }

  override def visitOC_Union(ctx: CypherParser.OC_UnionContext): Union = {
    Union(ctx.ALL, visitOC_SingleQuery(ctx.oC_SingleQuery))
  }

  override def visitOC_SinglePartQuery(ctx: CypherParser.OC_SinglePartQueryContext): SinglePartQuery = {
    visitEither(ctx).asInstanceOf[SinglePartQuery]
  }

  override def visitOC_ReadOnlyEnd(ctx: CypherParser.OC_ReadOnlyEndContext): ReadOnlyEnd = {
    ReadOnlyEnd(visitOC_ReadPart(ctx.oC_ReadPart), visitOC_Return(ctx.oC_Return))
  }

  override def visitOC_ReadPart(ctx: CypherParser.OC_ReadPartContext): ReadPart = {
    ReadPart(ctx.oC_ReadingClause.map(visitOC_ReadingClause))
  }

  override def visitOC_ReadingClause(ctx: CypherParser.OC_ReadingClauseContext): ReadingClause = {
    visitEither(ctx).asInstanceOf[ReadingClause]
  }

  override def visitOC_Return(ctx: CypherParser.OC_ReturnContext): Return = {
    Return(ctx.DISTINCT, visitOC_ReturnBody(ctx.oC_ReturnBody))
  }

  override def visitOC_ReturnBody(ctx: CypherParser.OC_ReturnBodyContext): ReturnBody = {
    //ReturnBody(visitOC_ReturnItems(ctx.oC_ReturnItems), Option(visitOC_Order(ctx.oC_Order)), Option(visitOC_Skip(ctx.oC_Skip)), Option(visitOC_Limit(ctx.oC_Limit)))
    ReturnBody(visitOC_ReturnItems(ctx.oC_ReturnItems), None, None, None)
  }

  override def visitOC_ReturnItems(ctx: CypherParser.OC_ReturnItemsContext): ReturnItems = {
    val star = ctx.contains("*")
    val items = ctx.oC_ReturnItem.map(visitOC_ReturnItem)
    ReturnItems(star, items)
  }

  override def visitOC_ReturnItem(ctx: CypherParser.OC_ReturnItemContext): ReturnItem = {
    val expr = visitOC_Expression(ctx.oC_Expression)
    if (ctx.AS != null) {
      Alias(expr, visitOC_Variable(ctx.oC_Variable))
    } else {
      expr
    }
  }

  override def visitOC_Expression(ctx: CypherParser.OC_ExpressionContext): Expression = {
    visitOC_OrExpression(ctx.oC_OrExpression)
  }

  override def visitOC_OrExpression(ctx: CypherParser.OC_OrExpressionContext): Expression = {
    val ors = ctx.oC_XorExpression.map(visitOC_XorExpression)
    ors match {
      case Nil => throw new IllegalArgumentException("Empty OR")
      case h :: Nil => h
      case _ => OrExpression(ors)
    }
  }

  override def visitOC_XorExpression(ctx: CypherParser.OC_XorExpressionContext): Expression = {
    val xors = ctx.oC_AndExpression.map(visitOC_AndExpression)
    xors match {
      case Nil => throw new IllegalArgumentException("Empty XOR")
      case h :: Nil => h
      case _ => XorExpression(xors)
    }
  }

  override def visitOC_AndExpression(ctx: CypherParser.OC_AndExpressionContext): Expression = {
    val ands = ctx.oC_NotExpression.map(visitOC_NotExpression)
    ands match {
      case Nil => throw new IllegalArgumentException("Empty AND")
      case h :: Nil => h
      case _ => AndExpression(ands)
    }
  }

  override def visitOC_NotExpression(ctx: CypherParser.OC_NotExpressionContext): Expression = {
    val expr = visitOC_ComparisonExpression(ctx.oC_ComparisonExpression)
    if (ctx.NOT.size % 2 != 0) {
      NotExpression(expr)
    } else {
      expr
    }
  }

  override def visitOC_ComparisonExpression(ctx: CypherParser.OC_ComparisonExpressionContext): Expression = {
    val expr = visitOC_AddOrSubtractExpression(ctx.oC_AddOrSubtractExpression)
    val comparisonExprs = ctx.oC_PartialComparisonExpression.asScala.toList
    comparisonExprs.foldLeft(expr: Expression) { case (left, partialComparisonExpr) =>
      val op = partialComparisonExpr.children.terminals("<=<>>=").head
      val right = visitOC_AddOrSubtractExpression(partialComparisonExpr.oC_AddOrSubtractExpression)
      op match {
        case "=" => EqualExpression(left, right)
        case "<>" => NotExpression(EqualExpression(left, right))
        case "<" => LessThanExpression(left, right)
        case ">" => NotExpression(LessThanOrEqualExpression(left, right))
        case "<=" => LessThanOrEqualExpression(left, right)
        case ">=" => NotExpression(LessThanExpression(left, right))
      }
    }
  }

  override def visitOC_AddOrSubtractExpression(ctx: CypherParser.OC_AddOrSubtractExpressionContext): AddOrSubtractExpression = {
    val exprs = ctx.oC_MultiplyDivideModuloExpression.map(visitOC_MultiplyDivideModuloExpression)
    val ops = ctx.children.terminals("+-")
    val opsWithExpr = ops zip exprs.tail
    opsWithExpr.foldLeft(exprs.head: AddOrSubtractExpression) { case (left, (op, right)) =>
      op match {
        case "+" => AddExpression(left, right)
        case "-" => SubtractExpression(left, right)
        case _ => throw new IllegalArgumentException("Unbalanced AddOrSubtractExpression")
      }
    }
  }

  override def visitOC_MultiplyDivideModuloExpression(ctx: CypherParser.OC_MultiplyDivideModuloExpressionContext): MultiplyDivideModuloExpression = {
    ???
  }

  //  override def visitOC_Match(ctx: CypherParser.OC_MatchContext): Match = {
  //    Match((ctx.getText, visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Pattern(ctx.oC_Pattern), ((visitOC_SP(ctx.oC_SP)).?, visitOC_Where(ctx.oC_Where)).?)
  //  }

  override def visitChildren(node: RuleNode): CypherAst = {
    println(node.getRuleContext.getClass.getSimpleName)
    //throw new RuntimeException(s"Not implemented: ${node.getRuleContext.getClass.getSimpleName.dropRight(7)}")
    null.asInstanceOf[CypherAst]
  }

  //  override def visitTerminal(node: TerminalNode): CypherAst = {
  //
  //  }

  override def visitOC_Variable(ctx: CypherParser.OC_VariableContext): Variable = {
    Variable(ctx.getText)
  }

  //  override def visitOC_ExponentDecimalReal(ctx: CypherParser.OC_ExponentDecimalRealContext): String = {
  //    ExponentDecimalReal(((ctx.getText) | ((visitOC_Digit(ctx.oC_Digit)).rep(min = 1)) | ((visitOC_Digit(ctx.oC_Digit)).rep(min = 1), ctx.getText, (visitOC_Digit(ctx.oC_Digit)).rep(min = 1)) | (ctx.getText, (visitOC_Digit(ctx.oC_Digit)).rep(min = 1))), ctx.getText, (ctx.getText).?, (visitOC_Digit(ctx.oC_Digit)).rep(min = 1))
  //  }
  //  override def visitOC_Properties(ctx: CypherParser.OC_PropertiesContext): Properties = {
  //    visitChildren(ctx).asInstanceOf[Properties]
  //  }
  //  override def visitOC_UnescapedSymbolicName(ctx: CypherParser.OC_UnescapedSymbolicNameContext): String = {
  //    UnescapedSymbolicName(visitOC_IdentifierStart(ctx.oC_IdentifierStart), (visitOC_IdentifierPart(ctx.oC_IdentifierPart)).rep)
  //  }
  //  override def visitOC_Set(ctx: CypherParser.OC_SetContext): Set = {
  //    Set(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_SetItem(ctx.oC_SetItem), (ctx.getText, visitOC_SetItem(ctx.oC_SetItem)).rep)
  //  }
  //  override def visitOC_RelationshipDetail(ctx: CypherParser.OC_RelationshipDetailContext): RelationshipDetail = {
  //    RelationshipDetail(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, (visitOC_Variable(ctx.oC_Variable), (visitOC_SP(ctx.oC_SP)).?).?, (visitOC_RelationshipTypes(ctx.oC_RelationshipTypes), (visitOC_SP(ctx.oC_SP)).?).?, (visitOC_RangeLiteral(ctx.oC_RangeLiteral)).?, (visitOC_Properties(ctx.oC_Properties), (visitOC_SP(ctx.oC_SP)).?).?, ctx.getText)
  //  }
  //  override def visitOC_Atom(ctx: CypherParser.OC_AtomContext): Atom = {
  //    visitChildren(ctx).asInstanceOf[Atom]
  //  }
  //  override def visitOC_HexInteger(ctx: CypherParser.OC_HexIntegerContext): String = {
  //    HexInteger("ctx.getText", (visitOC_HexDigit(ctx.oC_HexDigit)).rep(min = 1))
  //  }
  //  override def visitOC_CaseAlternatives(ctx: CypherParser.OC_CaseAlternativesContext): CaseAlternatives = {
  //    CaseAlternatives(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression))
  //  }
  //  override def visitOC_RelationshipPattern(ctx: CypherParser.OC_RelationshipPatternContext): RelationshipPattern = {
  //    visitChildren(ctx).asInstanceOf[RelationshipPattern]
  //  }
  //  override def visitOC_With(ctx: CypherParser.OC_WithContext): With = {
  //    With(ctx.getText, ((visitOC_SP(ctx.oC_SP)).?, ctx.getText).?, visitOC_SP(ctx.oC_SP), visitOC_ReturnBody(ctx.oC_ReturnBody), ((visitOC_SP(ctx.oC_SP)).?, visitOC_Where(ctx.oC_Where)).?)
  //  }
  //  override def visitOC_PatternElement(ctx: CypherParser.OC_PatternElementContext): PatternElement = {
  //    visitChildren(ctx).asInstanceOf[PatternElement]
  //  }
  //  override def visitOC_PropertyExpression(ctx: CypherParser.OC_PropertyExpressionContext): PropertyExpression = {
  //    PropertyExpression(visitOC_Atom(ctx.oC_Atom), ((visitOC_SP(ctx.oC_SP)).?, visitOC_PropertyLookup(ctx.oC_PropertyLookup)).rep(min = 1))
  //  }
  //  override def visitOC_RelationshipTypes(ctx: CypherParser.OC_RelationshipTypesContext): RelationshipTypes = {
  //    RelationshipTypes(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_RelTypeName(ctx.oC_RelTypeName), ((visitOC_SP(ctx.oC_SP)).?, ctx.getText, (ctx.getText).?, (visitOC_SP(ctx.oC_SP)).?, visitOC_RelTypeName(ctx.oC_RelTypeName)).rep)
  //  }
  //  override def visitOC_Parameter(ctx: CypherParser.OC_ParameterContext): Parameter = {
  //    visitChildren(ctx).asInstanceOf[Parameter]
  //  }
  //  override def visitOC_Pattern(ctx: CypherParser.OC_PatternContext): Pattern = {
  //    Pattern(visitOC_PatternPart(ctx.oC_PatternPart), ((visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_PatternPart(ctx.oC_PatternPart)).rep)
  //  }
  //  override def visitOC_PropertyOrLabelsExpression(ctx: CypherParser.OC_PropertyOrLabelsExpressionContext): PropertyOrLabelsExpression = {
  //    PropertyOrLabelsExpression(visitOC_Atom(ctx.oC_Atom), ((visitOC_SP(ctx.oC_SP)).?, visitOC_PropertyLookup(ctx.oC_PropertyLookup)).rep, ((visitOC_SP(ctx.oC_SP)).?, visitOC_NodeLabels(ctx.oC_NodeLabels)).?)
  //  }
  //  override def visitOC_PatternElementChain(ctx: CypherParser.OC_PatternElementChainContext): PatternElementChain = {
  //    PatternElementChain(visitOC_RelationshipPattern(ctx.oC_RelationshipPattern), (visitOC_SP(ctx.oC_SP)).?, visitOC_NodePattern(ctx.oC_NodePattern))
  //  }
  //  override def visitOC_NodePattern(ctx: CypherParser.OC_NodePatternContext): NodePattern = {
  //    NodePattern(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, (visitOC_Variable(ctx.oC_Variable), (visitOC_SP(ctx.oC_SP)).?).?, (visitOC_NodeLabels(ctx.oC_NodeLabels), (visitOC_SP(ctx.oC_SP)).?).?, (visitOC_Properties(ctx.oC_Properties), (visitOC_SP(ctx.oC_SP)).?).?, ctx.getText)
  //  }
  //  override def visitOC_EscapedSymbolicName(ctx: CypherParser.OC_EscapedSymbolicNameContext): String = {
  //    EscapedSymbolicName((ctx.getText, (ctx.getText).rep, ctx.getText).rep(min = 1))
  //  }
  //  override def visitOC_Variable(ctx: CypherParser.OC_VariableContext): Variable = {
  //    Variable(visitOC_SymbolicName(ctx.oC_SymbolicName))
  //  }
  //  override def visitOC_NodeLabel(ctx: CypherParser.OC_NodeLabelContext): NodeLabel = {
  //    NodeLabel(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_LabelName(ctx.oC_LabelName))
  //  }
  //  override def visitOC_SymbolicName(ctx: CypherParser.OC_SymbolicNameContext): SymbolicName = {
  //    visitChildren(ctx).asInstanceOf[SymbolicName]
  //  }
  //  override def visitOC_MultiPartQuery(ctx: CypherParser.OC_MultiPartQueryContext): MultiPartQuery = {
  //    MultiPartQuery(((ctx.getText) | (visitOC_ReadPart(ctx.oC_ReadPart)) | (visitOC_UpdatingStartClause(ctx.oC_UpdatingStartClause), (visitOC_SP(ctx.oC_SP)).?, visitOC_UpdatingPart(ctx.oC_UpdatingPart))), visitOC_With(ctx.oC_With), (visitOC_SP(ctx.oC_SP)).?, (visitOC_ReadPart(ctx.oC_ReadPart), visitOC_UpdatingPart(ctx.oC_UpdatingPart), visitOC_With(ctx.oC_With), (visitOC_SP(ctx.oC_SP)).?).rep, visitOC_SinglePartQuery(ctx.oC_SinglePartQuery))
  //  }
  //  override def visitOC_ReservedWord(ctx: CypherParser.OC_ReservedWordContext): ReservedWord = {
  //    visitChildren(ctx).asInstanceOf[ReservedWord]
  //  }
  //  override def visitOC_SortItem(ctx: CypherParser.OC_SortItemContext): SortItem = {
  //    SortItem(visitOC_Expression(ctx.oC_Expression), ((visitOC_SP(ctx.oC_SP)).?, ((ctx.getText) | (ctx.getText) | (ctx.getText) | (ctx.getText) | (ctx.getText))).?)
  //  }
  //  override def visitOC_Limit(ctx: CypherParser.OC_LimitContext): Limit = {
  //    Limit(ctx.getText, visitOC_SP(ctx.oC_SP), visitOC_Expression(ctx.oC_Expression))
  //  }
  //  override def visitOC_ParenthesizedExpression(ctx: CypherParser.OC_ParenthesizedExpressionContext): ParenthesizedExpression = {
  //    ParenthesizedExpression(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?, ctx.getText)
  //  }
  //  override def visitOC_RightArrowHead(ctx: CypherParser.OC_RightArrowHeadContext): String = {
  //    RightArrowHead(ctx.getText)
  //  }
  //  override def visitOC_Remove(ctx: CypherParser.OC_RemoveContext): Remove = {
  //    Remove(ctx.getText, visitOC_SP(ctx.oC_SP), visitOC_RemoveItem(ctx.oC_RemoveItem), ((visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_RemoveItem(ctx.oC_RemoveItem)).rep)
  //  }
  //  override def visitOC_DecimalInteger(ctx: CypherParser.OC_DecimalIntegerContext): String = {
  //    DecimalInteger(((ctx.getText) | (visitOC_ZeroDigit(ctx.oC_ZeroDigit)) | (visitOC_NonZeroDigit(ctx.oC_NonZeroDigit), (visitOC_Digit(ctx.oC_Digit)).rep)))
  //  }
  //  override def visitOC_StringLiteral(ctx: CypherParser.OC_StringLiteralContext): String = {
  //    StringLiteral(((ctx.getText) | (ctx.getText, (((ctx.getText) | (visitOC_EscapedChar(ctx.oC_EscapedChar)))).rep, ctx.getText) | (ctx.getText, (((ctx.getText) | (visitOC_EscapedChar(ctx.oC_EscapedChar)))).rep, ctx.getText)))
  //  }
  //  override def visitOC_Literal(ctx: CypherParser.OC_LiteralContext): Literal = {
  //    visitChildren(ctx).asInstanceOf[Literal]
  //  }
  //  override def visitOC_FunctionName(ctx: CypherParser.OC_FunctionNameContext): FunctionName = {
  //    visitChildren(ctx).asInstanceOf[FunctionName]
  //  }
  //  override def visitOC_SP(ctx: CypherParser.OC_SPContext): Unit = {
  //    SP((visitOC_whitespace(ctx.oC_whitespace)).rep(min = 1))
  //  }
  //  override def visitOC_PropertyLookup(ctx: CypherParser.OC_PropertyLookupContext): PropertyLookup = {
  //    visitChildren(ctx).asInstanceOf[PropertyLookup]
  //  }
  //  override def visitOC_NonZeroDigit(ctx: CypherParser.OC_NonZeroDigitContext): String = {
  //    NonZeroDigit(((ctx.getText) | (visitOC_NonZeroOctDigit(ctx.oC_NonZeroOctDigit))))
  //  }
  //  override def visitOC_NodeLabels(ctx: CypherParser.OC_NodeLabelsContext): NodeLabels = {
  //    NodeLabels(visitOC_NodeLabel(ctx.oC_NodeLabel), ((visitOC_SP(ctx.oC_SP)).?, visitOC_NodeLabel(ctx.oC_NodeLabel)).rep)
  //  }
  //  override def visitOC_NumberLiteral(ctx: CypherParser.OC_NumberLiteralContext): NumberLiteral = {
  //    visitChildren(ctx).asInstanceOf[NumberLiteral]
  //  }
  //  override def visitOC_FilterExpression(ctx: CypherParser.OC_FilterExpressionContext): FilterExpression = {
  //    FilterExpression(visitOC_IdInColl(ctx.oC_IdInColl), ((visitOC_SP(ctx.oC_SP)).?, visitOC_Where(ctx.oC_Where)).?)
  //  }
  //  override def visitOC_PatternComprehension(ctx: CypherParser.OC_PatternComprehensionContext): PatternComprehension = {
  //    PatternComprehension(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, (visitOC_Variable(ctx.oC_Variable), (visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?).?, visitOC_RelationshipsPattern(ctx.oC_RelationshipsPattern), (visitOC_SP(ctx.oC_SP)).?, (ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?, ctx.getText)
  //  }
  //  override def visitOC_UpdatingStartClause(ctx: CypherParser.OC_UpdatingStartClauseContext): UpdatingStartClause = {
  //    visitChildren(ctx).asInstanceOf[UpdatingStartClause]
  //  }
  //  override def visitOC_AnonymousPatternPart(ctx: CypherParser.OC_AnonymousPatternPartContext): AnonymousPatternPart = {
  //    visitChildren(ctx).asInstanceOf[AnonymousPatternPart]
  //  }
  //  override def visitOC_StringListNullOperatorExpression(ctx: CypherParser.OC_StringListNullOperatorExpressionContext): StringListNullOperatorExpression = {
  //    StringListNullOperatorExpression(visitOC_PropertyOrLabelsExpression(ctx.oC_PropertyOrLabelsExpression), (((ctx.getText) | ((visitOC_SP(ctx.oC_SP)).?, ctx.getText, visitOC_Expression(ctx.oC_Expression), ctx.getText) | ((visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_Expression(ctx.oC_Expression)).?, ctx.getText, (visitOC_Expression(ctx.oC_Expression)).?, ctx.getText) | (((ctx.getText) | (visitOC_SP(ctx.oC_SP), ctx.getText) | (visitOC_SP(ctx.oC_SP), ctx.getText, visitOC_SP(ctx.oC_SP), ctx.getText) | (visitOC_SP(ctx.oC_SP), ctx.getText, visitOC_SP(ctx.oC_SP), ctx.getText) | (visitOC_SP(ctx.oC_SP), ctx.getText)), (visitOC_SP(ctx.oC_SP)).?, visitOC_PropertyOrLabelsExpression(ctx.oC_PropertyOrLabelsExpression)) | (visitOC_SP(ctx.oC_SP), ctx.getText, visitOC_SP(ctx.oC_SP), ctx.getText) | (visitOC_SP(ctx.oC_SP), ctx.getText, visitOC_SP(ctx.oC_SP), ctx.getText, visitOC_SP(ctx.oC_SP), ctx.getText))).rep)
  //  }
  //  override def visitOC_YieldItem(ctx: CypherParser.OC_YieldItemContext): YieldItem = {
  //    YieldItem((visitOC_ProcedureResultField(ctx.oC_ProcedureResultField), visitOC_SP(ctx.oC_SP), ctx.getText, visitOC_SP(ctx.oC_SP)).?, visitOC_Variable(ctx.oC_Variable))
  //  }
  //  override def visitOC_PowerOfExpression(ctx: CypherParser.OC_PowerOfExpressionContext): PowerOfExpression = {
  //    PowerOfExpression(visitOC_UnaryAddOrSubtractExpression(ctx.oC_UnaryAddOrSubtractExpression), ((visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_UnaryAddOrSubtractExpression(ctx.oC_UnaryAddOrSubtractExpression)).rep)
  //  }
  //  override def visitOC_SetItem(ctx: CypherParser.OC_SetItemContext): SetItem = {
  //    visitChildren(ctx).asInstanceOf[SetItem]
  //  }
  //  override def visitOC_ProcedureName(ctx: CypherParser.OC_ProcedureNameContext): ProcedureName = {
  //    ProcedureName(visitOC_Namespace(ctx.oC_Namespace), visitOC_SymbolicName(ctx.oC_SymbolicName))
  //  }
  //  override def visitOC_RemoveItem(ctx: CypherParser.OC_RemoveItemContext): RemoveItem = {
  //    visitChildren(ctx).asInstanceOf[RemoveItem]
  //  }
  //  override def visitOC_Query(ctx: CypherParser.OC_QueryContext): Query = {
  //    visitChildren(ctx).asInstanceOf[Query]
  //  }
  //  override def visitOC_MergeAction(ctx: CypherParser.OC_MergeActionContext): MergeAction = {
  //    visitChildren(ctx).asInstanceOf[MergeAction]
  //  }
  //  override def visitOC_LabelName(ctx: CypherParser.OC_LabelNameContext): LabelName = {
  //    LabelName(visitOC_SchemaName(ctx.oC_SchemaName))
  //  }
  //  override def visitOC_ReadUpdateEnd(ctx: CypherParser.OC_ReadUpdateEndContext): ReadUpdateEnd = {
  //    ReadUpdateEnd(visitOC_ReadingClause(ctx.oC_ReadingClause), ((visitOC_SP(ctx.oC_SP)).?, visitOC_ReadingClause(ctx.oC_ReadingClause)).rep, ((visitOC_SP(ctx.oC_SP)).?, visitOC_UpdatingClause(ctx.oC_UpdatingClause)).rep(min = 1), ((visitOC_SP(ctx.oC_SP)).?, visitOC_Return(ctx.oC_Return)).?)
  //  }
  //  override def visitOC_ImplicitProcedureInvocation(ctx: CypherParser.OC_ImplicitProcedureInvocationContext): ImplicitProcedureInvocation = {
  //    ImplicitProcedureInvocation(visitOC_ProcedureName(ctx.oC_ProcedureName))
  //  }
  //  override def visitOC_Where(ctx: CypherParser.OC_WhereContext): Where = {
  //    Where(ctx.getText, visitOC_SP(ctx.oC_SP), visitOC_Expression(ctx.oC_Expression))
  //  }
  //  override def visitOC_RegularDecimalReal(ctx: CypherParser.OC_RegularDecimalRealContext): String = {
  //    RegularDecimalReal((visitOC_Digit(ctx.oC_Digit)).rep, ctx.getText, (visitOC_Digit(ctx.oC_Digit)).rep(min = 1))
  //  }
  //  override def visitOC_UpdatingEnd(ctx: CypherParser.OC_UpdatingEndContext): UpdatingEnd = {
  //    UpdatingEnd(visitOC_UpdatingStartClause(ctx.oC_UpdatingStartClause), ((visitOC_SP(ctx.oC_SP)).?, visitOC_UpdatingClause(ctx.oC_UpdatingClause)).rep, ((visitOC_SP(ctx.oC_SP)).?, visitOC_Return(ctx.oC_Return)).?)
  //  }
  //  override def visitOC_Delete(ctx: CypherParser.OC_DeleteContext): Delete = {
  //    Delete((ctx.getText, visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), ((visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression)).rep)
  //  }
  //  override def visitOC_RangeLiteral(ctx: CypherParser.OC_RangeLiteralContext): RangeLiteral = {
  //    RangeLiteral(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, (visitOC_IntegerLiteral(ctx.oC_IntegerLiteral), (visitOC_SP(ctx.oC_SP)).?).?, (ctx.getText, (visitOC_SP(ctx.oC_SP)).?, (visitOC_IntegerLiteral(ctx.oC_IntegerLiteral), (visitOC_SP(ctx.oC_SP)).?).?).?)
  //  }
  //  override def visitOC_UnaryAddOrSubtractExpression(ctx: CypherParser.OC_UnaryAddOrSubtractExpressionContext): UnaryAddOrSubtractExpression = {
  //    UnaryAddOrSubtractExpression((ctx.getText, (visitOC_SP(ctx.oC_SP)).?).rep, visitOC_StringListNullOperatorExpression(ctx.oC_StringListNullOperatorExpression))
  //  }
  //  override def visitOC_NonZeroOctDigit(ctx: CypherParser.OC_NonZeroOctDigitContext): String = {
  //    NonZeroOctDigit(ctx.getText)
  //  }
  //  override def visitOC_RelationshipsPattern(ctx: CypherParser.OC_RelationshipsPatternContext): RelationshipsPattern = {
  //    RelationshipsPattern(visitOC_NodePattern(ctx.oC_NodePattern), ((visitOC_SP(ctx.oC_SP)).?, visitOC_PatternElementChain(ctx.oC_PatternElementChain)).rep(min = 1))
  //  }
  //  override def visitOC_PatternPart(ctx: CypherParser.OC_PatternPartContext): PatternPart = {
  //    visitChildren(ctx).asInstanceOf[PatternPart]
  //  }
  //  override def visitOC_IntegerLiteral(ctx: CypherParser.OC_IntegerLiteralContext): IntegerLiteral = {
  //    visitChildren(ctx).asInstanceOf[IntegerLiteral]
  //  }
  //  override def visitOC_LeftArrowHead(ctx: CypherParser.OC_LeftArrowHeadContext): String = {
  //    LeftArrowHead(ctx.getText)
  //  }
  //  override def visitOC_ProcedureResultField(ctx: CypherParser.OC_ProcedureResultFieldContext): ProcedureResultField = {
  //    ProcedureResultField(visitOC_SymbolicName(ctx.oC_SymbolicName))
  //  }
  //  override def visitOC_DoubleLiteral(ctx: CypherParser.OC_DoubleLiteralContext): DoubleLiteral = {
  //    visitChildren(ctx).asInstanceOf[DoubleLiteral]
  //  }
  //  override def visitOC_RelTypeName(ctx: CypherParser.OC_RelTypeNameContext): RelTypeName = {
  //    RelTypeName(visitOC_SchemaName(ctx.oC_SchemaName))
  //  }
  //  override def visitOC_Namespace(ctx: CypherParser.OC_NamespaceContext): Namespace = {
  //    Namespace((visitOC_SymbolicName(ctx.oC_SymbolicName), ctx.getText).rep)
  //  }
  //  override def visitOC_ListLiteral(ctx: CypherParser.OC_ListLiteralContext): ListLiteral = {
  //    ListLiteral(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, (visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?, (ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?).rep).?, ctx.getText)
  //  }
  //  override def visitOC_OctalInteger(ctx: CypherParser.OC_OctalIntegerContext): String = {
  //    OctalInteger(visitOC_ZeroDigit(ctx.oC_ZeroDigit), (visitOC_OctDigit(ctx.oC_OctDigit)).rep(min = 1))
  //  }
  //  override def visitOC_HexDigit(ctx: CypherParser.OC_HexDigitContext): String = {
  //    HexDigit(((ctx.getText) | (visitOC_Digit(ctx.oC_Digit)) | (visitOC_HexLetter(ctx.oC_HexLetter))))
  //  }
  //  override def visitOC_SchemaName(ctx: CypherParser.OC_SchemaNameContext): SchemaName = {
  //    visitChildren(ctx).asInstanceOf[SchemaName]
  //  }
  //  override def visitOC_MapLiteral(ctx: CypherParser.OC_MapLiteralContext): MapLiteral = {
  //    MapLiteral(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, (visitOC_PropertyKeyName(ctx.oC_PropertyKeyName), (visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?, (ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_PropertyKeyName(ctx.oC_PropertyKeyName), (visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?).rep).?, ctx.getText)
  //  }
  //  override def visitOC_InQueryCall(ctx: CypherParser.OC_InQueryCallContext): InQueryCall = {
  //    InQueryCall(ctx.getText, visitOC_SP(ctx.oC_SP), visitOC_ExplicitProcedureInvocation(ctx.oC_ExplicitProcedureInvocation), ((visitOC_SP(ctx.oC_SP)).?, ctx.getText, visitOC_SP(ctx.oC_SP), visitOC_YieldItems(ctx.oC_YieldItems)).?)
  //  }
  //  override def visitOC_IdentifierStart(ctx: CypherParser.OC_IdentifierStartContext): String = {
  //    IdentifierStart(ctx.getText)
  //  }
  //  override def visitOC_YieldItems(ctx: CypherParser.OC_YieldItemsContext): YieldItems = {
  //    visitChildren(ctx).asInstanceOf[YieldItems]
  //  }
  //  override def visitOC_FunctionInvocation(ctx: CypherParser.OC_FunctionInvocationContext): FunctionInvocation = {
  //    FunctionInvocation(visitOC_FunctionName(ctx.oC_FunctionName), (visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, (ctx.getText, (visitOC_SP(ctx.oC_SP)).?).?, (visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?, (ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?).rep).?, ctx.getText)
  //  }
  //  override def visitOC_ZeroDigit(ctx: CypherParser.OC_ZeroDigitContext): String = {
  //    ZeroDigit(ctx.getText)
  //  }
  //  override def visitOC_Digit(ctx: CypherParser.OC_DigitContext): String = {
  //    Digit(((ctx.getText) | (visitOC_ZeroDigit(ctx.oC_ZeroDigit)) | (visitOC_NonZeroDigit(ctx.oC_NonZeroDigit))))
  //  }
  //  override def visitOC_OctDigit(ctx: CypherParser.OC_OctDigitContext): String = {
  //    OctDigit(((ctx.getText) | (visitOC_ZeroDigit(ctx.oC_ZeroDigit)) | (visitOC_NonZeroOctDigit(ctx.oC_NonZeroOctDigit))))
  //  }
  //  override def visitOC_Dash(ctx: CypherParser.OC_DashContext): String = {
  //    Dash(ctx.getText)
  //  }
  //  override def visitOC_Order(ctx: CypherParser.OC_OrderContext): Order = {
  //    Order(ctx.getText, visitOC_SP(ctx.oC_SP), ctx.getText, visitOC_SP(ctx.oC_SP), visitOC_SortItem(ctx.oC_SortItem), (ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_SortItem(ctx.oC_SortItem)).rep)
  //  }
  //  override def visitOC_Unwind(ctx: CypherParser.OC_UnwindContext): Unwind = {
  //    Unwind(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), visitOC_SP(ctx.oC_SP), ctx.getText, visitOC_SP(ctx.oC_SP), visitOC_Variable(ctx.oC_Variable))
  //  }
  //  override def visitOC_HexLetter(ctx: CypherParser.OC_HexLetterContext): String = {
  //    HexLetter(ctx.getText)
  //  }
  //  override def visitOC_IdInColl(ctx: CypherParser.OC_IdInCollContext): IdInColl = {
  //    IdInColl(visitOC_Variable(ctx.oC_Variable), visitOC_SP(ctx.oC_SP), ctx.getText, visitOC_SP(ctx.oC_SP), visitOC_Expression(ctx.oC_Expression))
  //  }
  //  override def visitOC_whitespace(ctx: CypherParser.OC_whitespaceContext): Unit = {
  //    whitespace(((ctx.getText) | (visitOC_Comment(ctx.oC_Comment))))
  //  }
  //  override def visitOC_StandaloneCall(ctx: CypherParser.OC_StandaloneCallContext): StandaloneCall = {
  //    StandaloneCall(ctx.getText, visitOC_SP(ctx.oC_SP), ((ctx.getText) | (visitOC_ExplicitProcedureInvocation(ctx.oC_ExplicitProcedureInvocation)) | (visitOC_ImplicitProcedureInvocation(ctx.oC_ImplicitProcedureInvocation))), (visitOC_SP(ctx.oC_SP), ctx.getText, visitOC_SP(ctx.oC_SP), visitOC_YieldItems(ctx.oC_YieldItems)).?)
  //  }
  //  override def visitOC_CaseExpression(ctx: CypherParser.OC_CaseExpressionContext): CaseExpression = {
  //    CaseExpression(((ctx.getText) | (ctx.getText, ((visitOC_SP(ctx.oC_SP)).?, visitOC_CaseAlternatives(ctx.oC_CaseAlternatives)).rep(min = 1)) | (ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), ((visitOC_SP(ctx.oC_SP)).?, visitOC_CaseAlternatives(ctx.oC_CaseAlternatives)).rep(min = 1))), ((visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression)).?, (visitOC_SP(ctx.oC_SP)).?, ctx.getText)
  //  }
  //  override def visitOC_EscapedChar(ctx: CypherParser.OC_EscapedCharContext): String = {
  //    EscapedChar(ctx.getText, ((ctx.getText) | (ctx.getText, (visitOC_HexDigit(ctx.oC_HexDigit)).rep(min = 4, max = 4)) | (ctx.getText, (visitOC_HexDigit(ctx.oC_HexDigit)).rep(min = 8, max = 8))))
  //  }
  //  override def visitOC_Match(ctx: CypherParser.OC_MatchContext): Match = {
  //    Match((ctx.getText, visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Pattern(ctx.oC_Pattern), ((visitOC_SP(ctx.oC_SP)).?, visitOC_Where(ctx.oC_Where)).?)
  //  }
  //  override def visitOC_Comment(ctx: CypherParser.OC_CommentContext): String = {
  //    Comment(((ctx.getText) | (ctx.getText, (((ctx.getText) | (ctx.getText, ctx.getText))).rep, ctx.getText) | (ctx.getText, (ctx.getText).rep, (ctx.getText).?, ctx.getText)))
  //  }
  //  override def visitOC_ExplicitProcedureInvocation(ctx: CypherParser.OC_ExplicitProcedureInvocationContext): ExplicitProcedureInvocation = {
  //    ExplicitProcedureInvocation(visitOC_ProcedureName(ctx.oC_ProcedureName), (visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, (visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?, (ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression), (visitOC_SP(ctx.oC_SP)).?).rep).?, ctx.getText)
  //  }
  //  override def visitOC_Create(ctx: CypherParser.OC_CreateContext): Create = {
  //    Create(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Pattern(ctx.oC_Pattern))
  //  }
  //  override def visitOC_UpdatingClause(ctx: CypherParser.OC_UpdatingClauseContext): UpdatingClause = {
  //    visitChildren(ctx).asInstanceOf[UpdatingClause]
  //  }
  //  override def visitOC_IdentifierPart(ctx: CypherParser.OC_IdentifierPartContext): String = {
  //    IdentifierPart(ctx.getText)
  //  }
  //  override def visitOC_BooleanLiteral(ctx: CypherParser.OC_BooleanLiteralContext): String = {
  //    BooleanLiteral(((ctx.getText) | (ctx.getText) | (ctx.getText)))
  //  }
  //  override def visitOC_UpdatingPart(ctx: CypherParser.OC_UpdatingPartContext): UpdatingPart = {
  //    UpdatingPart((visitOC_UpdatingClause(ctx.oC_UpdatingClause), (visitOC_SP(ctx.oC_SP)).?).rep)
  //  }
  //  override def visitOC_Skip(ctx: CypherParser.OC_SkipContext): Skip = {
  //    Skip(ctx.getText, visitOC_SP(ctx.oC_SP), visitOC_Expression(ctx.oC_Expression))
  //  }
  //  override def visitOC_ListComprehension(ctx: CypherParser.OC_ListComprehensionContext): ListComprehension = {
  //    ListComprehension(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_FilterExpression(ctx.oC_FilterExpression), ((visitOC_SP(ctx.oC_SP)).?, ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_Expression(ctx.oC_Expression)).?, (visitOC_SP(ctx.oC_SP)).?, ctx.getText)
  //  }
  //  override def visitOC_Merge(ctx: CypherParser.OC_MergeContext): Merge = {
  //    Merge(ctx.getText, (visitOC_SP(ctx.oC_SP)).?, visitOC_PatternPart(ctx.oC_PatternPart), (visitOC_SP(ctx.oC_SP), visitOC_MergeAction(ctx.oC_MergeAction)).rep)
  //  }
  //  override def visitOC_PropertyKeyName(ctx: CypherParser.OC_PropertyKeyNameContext): PropertyKeyName = {
  //    PropertyKeyName(visitOC_SchemaName(ctx.oC_SchemaName))
  //  }


}

trait AnonymousTrait

case class Cypher(statement: Statement) extends CypherAst

trait Expression extends CypherAst with ReturnItem

case class OrExpression(expressions: List[Expression]) extends CypherAst with Expression

case class XorExpression(expressions: List[Expression]) extends CypherAst with Expression

case class AndExpression(expressions: List[Expression]) extends CypherAst with Expression

case class NotExpression(expression: Expression) extends CypherAst with Expression

trait ComparisonExpression extends CypherAst with Expression

case class EqualExpression(left: Expression, right: Expression) extends CypherAst with ComparisonExpression

case class LessThanExpression(left: Expression, right: Expression) extends CypherAst with ComparisonExpression

case class LessThanOrEqualExpression(left: Expression, right: Expression) extends CypherAst with ComparisonExpression

case class AddExpression(left: Expression, right: Expression) extends CypherAst with AddOrSubtractExpression

case class SubtractExpression(left: Expression, right: Expression) extends CypherAst with AddOrSubtractExpression

trait AddOrSubtractExpression extends CypherAst with Expression

case class MultiplyDivideModuloExpression(powerOfExpression: PowerOfExpression, stuff: List[Nothing]) extends CypherAst with Expression with AddOrSubtractExpression

case class PowerOfExpression(unaryAddOrSubtractExpressions: List[UnaryAddOrSubtractExpression]) extends CypherAst

case class UnaryAddOrSubtractExpression(strings: List[String], stringListNullOperatorExpression: StringListNullOperatorExpression) extends CypherAst

case class StringListNullOperatorExpression(propertyOrLabelsExpression: PropertyOrLabelsExpression, anonymousTraits: List[AnonymousTrait]) extends CypherAst

trait Properties extends CypherAst

case class Alias(expr: Expression, as: Variable) extends ReturnItem

case class Set(setItems: List[SetItem]) extends CypherAst with MergeAction with UpdatingClause

case class RelationshipDetail(
  maybeVariable: Option[Variable],
  maybeRelationshipTypes: Option[RelationshipTypes],
  maybeRangeLiteral: Option[RangeLiteral],
  maybeProperties: Option[Properties]) extends CypherAst {

  override val children = (maybeVariable ++ maybeRelationshipTypes ++ maybeRangeLiteral ++ maybeProperties).toArray

  override def withNewChildren(newChildren: Array[CypherAst]): CypherAst = {
    val v = newChildren.collectFirst { case v: Variable => v }
    val t = newChildren.collectFirst { case t: RelationshipTypes => t }
    val r = newChildren.collectFirst { case r: RangeLiteral => r }
    val p = newChildren.collectFirst { case p: Properties => p }
    copy(v, t, r, p)
  }

}

trait Atom extends CypherAst

case class Return(distinct: Boolean, returnBody: ReturnBody) extends CypherAst

case class CaseAlternatives(whenExpr: Expression, thenExpr: Expression) extends CypherAst

trait RelationshipPattern extends CypherAst

case class With(distinct: Boolean, returnBody: ReturnBody, maybeWhere: Option[Where]) extends CypherAst {

  override val children = (returnBody ++ maybeWhere).toArray

  override def withNewChildren(newChildren: Array[CypherAst]): CypherAst = {
    copy(distinct, newChildren(0).asInstanceOf[ReturnBody], if (newChildren.size == 2) Some(newChildren(1).asInstanceOf[Where]) else None)
  }

}

trait PatternElement extends CypherAst with AnonymousPatternPart

case class PropertyExpression(atom: Atom, propertyLookups: List[PropertyLookup]) extends CypherAst with RemoveItem

case class RelationshipTypes(relTypeNames: List[RelTypeName]) extends CypherAst

trait Parameter extends CypherAst with Properties with Atom

case class Pattern(patternParts: List[PatternPart]) extends CypherAst

case class PropertyOrLabelsExpression(atom: Atom, propertyLookups: List[PropertyLookup], maybeNodeLabels: Option[NodeLabels]) extends CypherAst {

  override val children = ((atom +: propertyLookups) ++ maybeNodeLabels).toArray

  override def withNewChildren(newChildren: Array[CypherAst]): CypherAst = {
    copy(newChildren(0).asInstanceOf[Atom], newChildren.collect { case p: PropertyLookup => p }.toList, if (newChildren.size == 3) Some(newChildren(2).asInstanceOf[NodeLabels]) else None)
  }

}

case class PatternElementChain(relationshipPattern: RelationshipPattern, nodePattern: NodePattern) extends CypherAst

case class ReadOnlyEnd(readPart: ReadPart, returnClause: Return) extends CypherAst with SinglePartQuery

trait SinglePartQuery extends CypherAst with SingleQuery

case class NodePattern(maybeVariable: Option[Variable], maybeNodeLabels: Option[NodeLabels], maybeProperties: Option[Properties]) extends CypherAst {

  override val children = (maybeVariable ++ maybeNodeLabels ++ maybeProperties).toArray

  override def withNewChildren(newChildren: Array[CypherAst]): CypherAst = {
    val v = newChildren.collectFirst { case v: Variable => v }
    val n = newChildren.collectFirst { case n: NodeLabels => n }
    val p = newChildren.collectFirst { case p: Properties => p }
    copy(v, n, p)
  }

}

case class Variable(name: String) extends CypherAst with Atom

case class NodeLabel(labelName: LabelName) extends CypherAst

case class SymbolicName(value: String) extends CypherAst with Parameter with FunctionName with SchemaName

case class MultiPartQuery(anonymousTrait: AnonymousTrait, withClause: With, tuple3s: List[(ReadPart, UpdatingPart, With)], singlePartQuery: SinglePartQuery) extends CypherAst with SingleQuery

trait ReservedWord extends CypherAst with SchemaName

case class SortItem(expression: Expression, maybeString: Option[String]) extends CypherAst

case class Limit(expression: Expression) extends CypherAst

case class ParenthesizedExpression(expression: Expression) extends CypherAst with Atom

case class ReturnItems(star: Boolean, returnItems: List[ReturnItem]) extends CypherAst

case class Remove(removeItems: List[RemoveItem]) extends CypherAst with UpdatingClause

trait Literal extends CypherAst with Atom

trait FunctionName extends CypherAst

trait PropertyLookup extends CypherAst

case class NodeLabels(nodeLabels: List[NodeLabel]) extends CypherAst

trait NumberLiteral extends CypherAst with Literal

case class FilterExpression(idInColl: IdInColl, maybeWhere: Option[Where]) extends CypherAst with Atom

case class PatternComprehension(maybeVariable: Option[Variable], relationshipsPattern: RelationshipsPattern, maybeExpression: Option[Expression], expression: Expression) extends CypherAst with Atom

trait UpdatingStartClause extends CypherAst

trait AnonymousPatternPart extends CypherAst with PatternPart

trait SingleQuery extends CypherAst

case class YieldItem(maybeProcedureResultField: Option[ProcedureResultField], variable: Variable) extends CypherAst

trait SetItem extends CypherAst

case class ProcedureName(namespace: Namespace, symbolicName: SymbolicName) extends CypherAst

trait RemoveItem extends CypherAst

trait Query extends CypherAst with Statement

trait MergeAction extends CypherAst

case class LabelName(schemaName: SchemaName) extends CypherAst

case class ReadUpdateEnd(readingClause: ReadingClause, readingClauses: List[ReadingClause], updatingClauses: List[UpdatingClause], maybeReturn: Option[Return]) extends CypherAst with SinglePartQuery

case class ImplicitProcedureInvocation(procedureName: ProcedureName) extends CypherAst

case class Where(expression: Expression) extends CypherAst

trait ReturnItem extends CypherAst

case class UpdatingEnd(updatingStartClause: UpdatingStartClause, updatingClauses: List[UpdatingClause], maybeReturn: Option[Return]) extends CypherAst with SinglePartQuery

case class Delete(expressions: List[Expression]) extends CypherAst with UpdatingClause

case class RangeLiteral(maybeIntegerLiteral: Option[IntegerLiteral], maybeMaybeIntegerLiteral: Option[Option[IntegerLiteral]]) extends CypherAst

case class RelationshipsPattern(nodePattern: NodePattern, patternElementChains: List[PatternElementChain]) extends CypherAst with Atom

trait PatternPart extends CypherAst

trait IntegerLiteral extends CypherAst with NumberLiteral

trait Statement extends CypherAst

case class ReadPart(readingClauses: List[ReadingClause]) extends CypherAst

trait ReadingClause extends CypherAst

case class ProcedureResultField(symbolicName: SymbolicName) extends CypherAst

trait DoubleLiteral extends CypherAst with NumberLiteral

case class RelTypeName(schemaName: SchemaName) extends CypherAst

case class Namespace(symbolicNames: List[SymbolicName]) extends CypherAst

case class ListLiteral(maybeListLiteral: Option[List[Expression]]) extends CypherAst with Literal

trait SchemaName extends CypherAst

case class MapLiteral(maybeMapLiteral: Option[(PropertyKeyName, Expression, List[(PropertyKeyName, Expression)])]) extends CypherAst with Properties with Literal

case class InQueryCall(explicitProcedureInvocation: ExplicitProcedureInvocation, maybeYieldItems: Option[YieldItems]) extends CypherAst with ReadingClause

trait YieldItems extends CypherAst

case class FunctionInvocation(functionName: FunctionName, distinct: Boolean, maybeExpressions: Option[List[Expression]]) extends CypherAst with Atom

case class ReturnBody(returnItems: ReturnItems, maybeOrder: Option[Order], maybeSkip: Option[Skip], maybeLimit: Option[Limit]) extends CypherAst

case class Order(sortItems: List[SortItem]) extends CypherAst

case class Unwind(expression: Expression, variable: Variable) extends CypherAst with ReadingClause

case class Union(all: Boolean, singleQuery: SingleQuery) extends CypherAst

case class IdInColl(variable: Variable, expression: Expression) extends CypherAst

case class StandaloneCall(anonymousTrait: AnonymousTrait, maybeYieldItems: Option[YieldItems]) extends CypherAst with Query

case class CaseExpression(anonymousTrait: AnonymousTrait, maybeExpression: Option[Expression]) extends CypherAst with Atom

case class Match(pattern: Pattern, maybeWhere: Option[Where]) extends CypherAst with ReadingClause

case class ExplicitProcedureInvocation(procedureName: ProcedureName, maybeExpressions: Option[List[Expression]]) extends CypherAst

case class Create(pattern: Pattern) extends CypherAst with UpdatingStartClause with UpdatingClause

trait UpdatingClause extends CypherAst

case class UpdatingPart(updatingClauses: List[UpdatingClause]) extends CypherAst

case class Skip(expression: Expression) extends CypherAst

case class ListComprehension(filterExpression: FilterExpression, maybeExpression: Option[Expression]) extends CypherAst with Atom

case class Merge(patternPart: PatternPart, mergeActions: List[MergeAction]) extends CypherAst with UpdatingStartClause with UpdatingClause

case class RegularQuery(singleQuery: SingleQuery, unions: List[Union]) extends CypherAst with Query

case class PropertyKeyName(schemaName: SchemaName) extends CypherAst with PropertyLookup
