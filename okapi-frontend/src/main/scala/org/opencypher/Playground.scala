package org.opencypher

import org.opencypher.okapi.trees.AbstractTreeNode

abstract class CypherAst extends AbstractTreeNode[CypherAst]
//
//abstract class Properties extends CypherAst
///**
//Grammar expression:
//|-Rule(Properties, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-RuleRef(MapLiteral)
//· · |-RuleRef(Parameter)
//  */
//case class Set(setItems: List[SetItem]) extends CypherAst
///**
//Grammar expression:
//|-Rule(Set, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, SET)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RepeatWithSeparator(1, None, Some(setItems))
//· · · |-RuleRef(SetItem)
//· · · |-Sequence(None)
//· · · · |-IgnoreCaseLiteral(None, ,)
//  */
//case class RelationshipDetail(variableOpt: Option[Variable], relationshipTypesOpt: Option[RelationshipTypes], rangeLiteralOpt: Option[RangeLiteral], propertiesOpt: Option[Properties]) extends CypherAst
///**
//Grammar expression:
//|-Rule(RelationshipDetail, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, [)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(Variable)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(RelationshipTypes)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-RuleRef(RangeLiteral)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(Properties)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, ])
//  */
//abstract class Atom extends CypherAst
///**
//Grammar expression:
//|-Rule(Atom, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-RuleRef(Literal)
//· · |-RuleRef(Parameter)
//· · |-RuleRef(CaseExpression)
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, COUNT)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ()
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, *)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ))
//· · |-RuleRef(ListComprehension)
//· · |-RuleRef(PatternComprehension)
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, FILTER)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ()
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(FilterExpression)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ))
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, EXTRACT)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ()
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(FilterExpression)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-Optional(None)
//· · · · |-Sequence(None)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, |)
//· · · · · |-RuleRef(Expression)
//· · · |-IgnoreCaseLiteral(None, ))
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, ALL)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ()
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(FilterExpression)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ))
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, ANY)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ()
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(FilterExpression)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ))
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, NONE)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ()
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(FilterExpression)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ))
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, SINGLE)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ()
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(FilterExpression)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ))
//· · |-RuleRef(RelationshipsPattern)
//· · |-RuleRef(ParenthesizedExpression)
//· · |-RuleRef(FunctionInvocation)
//· · |-RuleRef(Variable)
//  */
//case object COUNT extends Atom
//case class Return(returnBody: ReturnBody) extends CypherAst
///**
//Grammar expression:
//|-Rule(Return, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, RETURN)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, DISTINCT)
//· · |-RuleRef(SP)
//· · |-RuleRef(ReturnBody)
//  */
//case class CaseAlternatives(expression: Expression, expression: Expression) extends CypherAst
///**
//Grammar expression:
//|-Rule(CaseAlternatives, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, WHEN)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(Expression)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, THEN)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(Expression)
//  */
//case class OrExpression(xorExpressions: List[XorExpression]) extends Expression
///**
//Grammar expression:
//|-Rule(OrExpression, Some(Expression), true, false)
//· |-RepeatWithSeparator(1, None, Some(xorExpressions))
//· · |-RuleRef(XorExpression)
//· · |-Sequence(None)
//· · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, OR)
//· · · |-RuleRef(SP)
//  */
//abstract class RelationshipPattern extends CypherAst
///**
//Grammar expression:
//|-Rule(RelationshipPattern, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-Sequence(None)
//· · · |-RuleRef(LeftArrowHead)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(Dash)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-Optional(None)
//· · · · |-RuleRef(RelationshipDetail)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(Dash)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(RightArrowHead)
//· · |-Sequence(None)
//· · · |-RuleRef(LeftArrowHead)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(Dash)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-Optional(None)
//· · · · |-RuleRef(RelationshipDetail)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(Dash)
//· · |-Sequence(None)
//· · · |-RuleRef(Dash)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-Optional(None)
//· · · · |-RuleRef(RelationshipDetail)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(Dash)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(RightArrowHead)
//· · |-Sequence(None)
//· · · |-RuleRef(Dash)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-Optional(None)
//· · · · |-RuleRef(RelationshipDetail)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(Dash)
//  */
//case class RelationshipPatternImpl21(()) extends RelationshipPattern
//case class RelationshipPatternImpl87(()) extends RelationshipPattern
//case class RelationshipPatternImpl10(()) extends RelationshipPattern
//case class RelationshipPatternImpl61(()) extends RelationshipPattern
//case class With(returnBody: ReturnBody, whereOpt: Option[Where]) extends CypherAst
///**
//Grammar expression:
//|-Rule(With, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, WITH)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, DISTINCT)
//· · |-RuleRef(SP)
//· · |-RuleRef(ReturnBody)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Where)
//  */
//abstract class PatternElement extends CypherAst
///**
//Grammar expression:
//|-Rule(PatternElement, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-Sequence(None)
//· · · |-RuleRef(NodePattern)
//· · · |-Repeat(0, None, None)
//· · · · |-Sequence(None)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-RuleRef(PatternElementChain)
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, ()
//· · · |-RuleRef(PatternElement)
//· · · |-IgnoreCaseLiteral(None, ))
//  */
//case class PatternElementImpl74(()) extends PatternElement
//case class PropertyExpression(atom: Atom, propertyLookups: List[PropertyLookup]) extends CypherAst
///**
//Grammar expression:
//|-Rule(PropertyExpression, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-RuleRef(Atom)
//· · |-Repeat(1, None, None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(PropertyLookup)
//  */
//case class RelationshipTypes(relTypeNames: List[RelTypeName]) extends CypherAst
///**
//Grammar expression:
//|-Rule(RelationshipTypes, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, :)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RepeatWithSeparator(1, None, Some(relTypeNames))
//· · · |-RuleRef(RelTypeName)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, |)
//· · · · |-Optional(None)
//· · · · · |-IgnoreCaseLiteral(None, :)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//  */
//case class Parameter(generatedAbstractClass22: GeneratedAbstractClass22) extends CypherAst
///**
//Grammar expression:
//|-Rule(Parameter, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, $)
//· · |-Either(None)
//· · · |-RuleRef(SymbolicName)
//· · · |-RuleRef(DecimalInteger)
//  */
//case class Pattern(patternParts: List[PatternPart]) extends CypherAst
///**
//Grammar expression:
//|-Rule(Pattern, Some(CypherAst), false, false)
//· |-RepeatWithSeparator(1, None, Some(patternParts))
//· · |-RuleRef(PatternPart)
//· · |-Sequence(None)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ,)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//  */
//case class PropertyOrLabelsExpression(atom: Atom, propertyLookups: List[PropertyLookup], nodeLabelsOpt: Option[NodeLabels]) extends CypherAst
///**
//Grammar expression:
//|-Rule(PropertyOrLabelsExpression, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(Atom)
//· · |-Repeat(0, None, None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(PropertyLookup)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(NodeLabels)
//  */
//case class PatternElementChain(relationshipPattern: RelationshipPattern, nodePattern: NodePattern) extends CypherAst
///**
//Grammar expression:
//|-Rule(PatternElementChain, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(RelationshipPattern)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(NodePattern)
//  */
//case class ReadOnlyEnd(readPart: ReadPart, return: Return) extends CypherAst
///**
//Grammar expression:
//|-Rule(ReadOnlyEnd, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(ReadPart)
//· · |-RuleRef(Return)
//  */
//abstract class SinglePartQuery extends CypherAst
///**
//Grammar expression:
//|-Rule(SinglePartQuery, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-RuleRef(ReadOnlyEnd)
//· · |-RuleRef(ReadUpdateEnd)
//· · |-RuleRef(UpdatingEnd)
//  */
//case class NodePattern(variableOpt: Option[Variable], nodeLabelsOpt: Option[NodeLabels], propertiesOpt: Option[Properties]) extends CypherAst
///**
//Grammar expression:
//|-Rule(NodePattern, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, ()
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(Variable)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(NodeLabels)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(Properties)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, ))
//  */
//case class Variable(symbolicName: SymbolicName) extends CypherAst
///**
//Grammar expression:
//|-Rule(Variable, Some(CypherAst), true, false)
//· |-RuleRef(SymbolicName)
//  */
//case class NodeLabel(labelName: LabelName) extends NodeLabels
///**
//Grammar expression:
//|-Rule(NodeLabel, Some(NodeLabels), true, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, :)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(LabelName)
//  */
//case class Cypher(statement: Statement) extends CypherAst
///**
//Grammar expression:
//|-Rule(Cypher, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(Statement)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, ;)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Fragment(Some(EOI), Set(), Set(EOI), Set())
//  */
//abstract class SymbolicName extends CypherAst
///**
//Grammar expression:
//|-Rule(SymbolicName, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-RuleRef(UnescapedSymbolicName)
//· · |-RuleRef(EscapedSymbolicName)
//· · |-RuleRef(HexLetter)
//· · |-IgnoreCaseLiteral(None, COUNT)
//· · |-IgnoreCaseLiteral(None, FILTER)
//· · |-IgnoreCaseLiteral(None, EXTRACT)
//· · |-IgnoreCaseLiteral(None, ANY)
//· · |-IgnoreCaseLiteral(None, NONE)
//· · |-IgnoreCaseLiteral(None, SINGLE)
//  */
//case object COUNT extends SymbolicName
//case object FILTER extends SymbolicName
//case object EXTRACT extends SymbolicName
//case object ANY extends SymbolicName
//case object NONE extends SymbolicName
//case object SINGLE extends SymbolicName
//case class MultiPartQuery(generatedAbstractClass49: GeneratedAbstractClass49, with: With, generatedAbstractClass51s: List[GeneratedAbstractClass51], singlePartQuery: SinglePartQuery) extends CypherAst
///**
//Grammar expression:
//|-Rule(MultiPartQuery, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-Either(None)
//· · · |-RuleRef(ReadPart)
//· · · |-Sequence(None)
//· · · · |-RuleRef(UpdatingStartClause)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(UpdatingPart)
//· · |-RuleRef(With)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Repeat(0, None, None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(ReadPart)
//· · · · |-RuleRef(UpdatingPart)
//· · · · |-RuleRef(With)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-RuleRef(SinglePartQuery)
//  */
//case class ReservedWord(reservedWord: String) extends CypherAst
///**
//Grammar expression:
//|-Rule(ReservedWord, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-IgnoreCaseLiteral(None, ALL)
//· · |-IgnoreCaseLiteral(None, ASC)
//· · |-IgnoreCaseLiteral(None, ASCENDING)
//· · |-IgnoreCaseLiteral(None, BY)
//· · |-IgnoreCaseLiteral(None, CREATE)
//· · |-IgnoreCaseLiteral(None, DELETE)
//· · |-IgnoreCaseLiteral(None, DESC)
//· · |-IgnoreCaseLiteral(None, DESCENDING)
//· · |-IgnoreCaseLiteral(None, DETACH)
//· · |-IgnoreCaseLiteral(None, EXISTS)
//· · |-IgnoreCaseLiteral(None, LIMIT)
//· · |-IgnoreCaseLiteral(None, MATCH)
//· · |-IgnoreCaseLiteral(None, MERGE)
//· · |-IgnoreCaseLiteral(None, ON)
//· · |-IgnoreCaseLiteral(None, OPTIONAL)
//· · |-IgnoreCaseLiteral(None, ORDER)
//· · |-IgnoreCaseLiteral(None, REMOVE)
//· · |-IgnoreCaseLiteral(None, RETURN)
//· · |-IgnoreCaseLiteral(None, SET)
//· · |-IgnoreCaseLiteral(None, SKIP)
//· · |-IgnoreCaseLiteral(None, WHERE)
//· · |-IgnoreCaseLiteral(None, WITH)
//· · |-IgnoreCaseLiteral(None, UNION)
//· · |-IgnoreCaseLiteral(None, UNWIND)
//· · |-IgnoreCaseLiteral(None, AND)
//· · |-IgnoreCaseLiteral(None, AS)
//· · |-IgnoreCaseLiteral(None, CONTAINS)
//· · |-IgnoreCaseLiteral(None, DISTINCT)
//· · |-IgnoreCaseLiteral(None, ENDS)
//· · |-IgnoreCaseLiteral(None, IN)
//· · |-IgnoreCaseLiteral(None, IS)
//· · |-IgnoreCaseLiteral(None, NOT)
//· · |-IgnoreCaseLiteral(None, OR)
//· · |-IgnoreCaseLiteral(None, STARTS)
//· · |-IgnoreCaseLiteral(None, XOR)
//· · |-IgnoreCaseLiteral(None, FALSE)
//· · |-IgnoreCaseLiteral(None, TRUE)
//· · |-IgnoreCaseLiteral(None, NULL)
//· · |-IgnoreCaseLiteral(None, CONSTRAINT)
//· · |-IgnoreCaseLiteral(None, DO)
//· · |-IgnoreCaseLiteral(None, FOR)
//· · |-IgnoreCaseLiteral(None, REQUIRE)
//· · |-IgnoreCaseLiteral(None, UNIQUE)
//· · |-IgnoreCaseLiteral(None, CASE)
//· · |-IgnoreCaseLiteral(None, WHEN)
//· · |-IgnoreCaseLiteral(None, THEN)
//· · |-IgnoreCaseLiteral(None, ELSE)
//· · |-IgnoreCaseLiteral(None, END)
//· · |-IgnoreCaseLiteral(None, MANDATORY)
//· · |-IgnoreCaseLiteral(None, SCALAR)
//· · |-IgnoreCaseLiteral(None, OF)
//· · |-IgnoreCaseLiteral(None, ADD)
//· · |-IgnoreCaseLiteral(None, DROP)
//  */
//case class SortItem(expression: Expression, generatedAbstractClass94Opt: Option[GeneratedAbstractClass94]) extends CypherAst
///**
//Grammar expression:
//|-Rule(SortItem, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(Expression)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-Either(None)
//· · · · · |-IgnoreCaseLiteral(None, ASCENDING)
//· · · · · |-IgnoreCaseLiteral(None, ASC)
//· · · · · |-IgnoreCaseLiteral(None, DESCENDING)
//· · · · · |-IgnoreCaseLiteral(None, DESC)
//  */
//case class Limit(expression: Expression) extends CypherAst
///**
//Grammar expression:
//|-Rule(Limit, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, LIMIT)
//· · |-RuleRef(SP)
//· · |-RuleRef(Expression)
//  */
//case class ParenthesizedExpression(expression: Expression) extends CypherAst
///**
//Grammar expression:
//|-Rule(ParenthesizedExpression, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, ()
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(Expression)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, ))
//  */
//abstract class ReturnItems extends CypherAst
///**
//Grammar expression:
//|-Rule(ReturnItems, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, *)
//· · · |-Repeat(0, None, None)
//· · · · |-Sequence(None)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, ,)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-RuleRef(ReturnItem)
//· · |-RepeatWithSeparator(1, None, Some(returnItems))
//· · · |-RuleRef(ReturnItem)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, ,)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//  */
//case class RightArrowHead(rightArrowHead: String) extends CypherAst
///**
//Grammar expression:
//|-Rule(RightArrowHead, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-IgnoreCaseLiteral(None, >)
//· · |-IgnoreCaseLiteral(None, ⟩)
//· · |-IgnoreCaseLiteral(None, 〉)
//· · |-IgnoreCaseLiteral(None, ﹥)
//· · |-IgnoreCaseLiteral(None, ＞)
//  */
//case class Remove(removeItems: List[RemoveItem]) extends CypherAst
///**
//Grammar expression:
//|-Rule(Remove, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, REMOVE)
//· · |-RuleRef(SP)
//· · |-RepeatWithSeparator(1, None, Some(removeItems))
//· · · |-RuleRef(RemoveItem)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, ,)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//  */
//abstract class Literal extends CypherAst
///**
//Grammar expression:
//|-Rule(Literal, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-RuleRef(NumberLiteral)
//· · |-RuleRef(StringLiteral)
//· · |-RuleRef(BooleanLiteral)
//· · |-IgnoreCaseLiteral(None, NULL)
//· · |-RuleRef(MapLiteral)
//· · |-RuleRef(ListLiteral)
//  */
//case object NULL extends Literal
//abstract class FunctionName extends CypherAst
///**
//Grammar expression:
//|-Rule(FunctionName, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-RuleRef(SymbolicName)
//· · |-IgnoreCaseLiteral(None, EXISTS)
//  */
//case object EXISTS extends FunctionName
//case class PropertyLookup(generatedAbstractClass21: GeneratedAbstractClass21) extends CypherAst
///**
//Grammar expression:
//|-Rule(PropertyLookup, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, .)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Either(None)
//· · · |-RuleRef(PropertyKeyName)
//  */
//abstract class Expression extends CypherAst
///**
//Grammar expression:
//|-Rule(Expression, Some(CypherAst), false, false)
//· |-RuleRef(OrExpression)
//  */
//case class NodeLabels(nodeLabels: List[NodeLabel]) extends CypherAst
///**
//Grammar expression:
//|-Rule(NodeLabels, Some(CypherAst), true, false)
//· |-RepeatWithSeparator(1, None, Some(nodeLabels))
//· · |-RuleRef(NodeLabel)
//· · |-Sequence(None)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//  */
//abstract class NumberLiteral extends CypherAst
///**
//Grammar expression:
//|-Rule(NumberLiteral, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-RuleRef(DoubleLiteral)
//· · |-RuleRef(IntegerLiteral)
//  */
//case class FilterExpression(idInColl: IdInColl, whereOpt: Option[Where]) extends CypherAst
///**
//Grammar expression:
//|-Rule(FilterExpression, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-RuleRef(IdInColl)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Where)
//  */
//case class PatternComprehension(variableOpt: Option[Variable], relationshipsPattern: RelationshipsPattern, expressionOpt: Option[Expression], expression: Expression) extends CypherAst
///**
//Grammar expression:
//|-Rule(PatternComprehension, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, [)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(Variable)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, =)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-RuleRef(RelationshipsPattern)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-IgnoreCaseLiteral(None, WHERE)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Expression)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, |)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(Expression)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, ])
//  */
//abstract class UpdatingStartClause extends CypherAst
///**
//Grammar expression:
//|-Rule(UpdatingStartClause, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-RuleRef(Create)
//· · |-RuleRef(Merge)
//  */
//abstract class AnonymousPatternPart extends CypherAst
///**
//Grammar expression:
//|-Rule(AnonymousPatternPart, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-RuleRef(PatternElement)
//  */
//abstract class SingleQuery extends CypherAst
///**
//Grammar expression:
//|-Rule(SingleQuery, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-RuleRef(SinglePartQuery)
//· · |-RuleRef(MultiPartQuery)
//  */
//case class StringListNullOperatorExpression(propertyOrLabelsExpression: PropertyOrLabelsExpression, generatedAbstractClass81s: List[GeneratedAbstractClass81]) extends CypherAst
///**
//Grammar expression:
//|-Rule(StringListNullOperatorExpression, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(PropertyOrLabelsExpression)
//· · |-Repeat(0, None, None)
//· · · |-Either(None)
//· · · · |-Sequence(None)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, [)
//· · · · · |-RuleRef(Expression)
//· · · · · |-IgnoreCaseLiteral(None, ])
//· · · · |-Sequence(None)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, [)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(Expression)
//· · · · · |-IgnoreCaseLiteral(None, ..)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(Expression)
//· · · · · |-IgnoreCaseLiteral(None, ])
//· · · · |-Sequence(None)
//· · · · · |-Either(None)
//· · · · · · |-Sequence(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · · |-IgnoreCaseLiteral(None, IN)
//· · · · · · |-Sequence(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · · |-IgnoreCaseLiteral(None, STARTS)
//· · · · · · · |-RuleRef(SP)
//· · · · · · · |-IgnoreCaseLiteral(None, WITH)
//· · · · · · |-Sequence(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · · |-IgnoreCaseLiteral(None, ENDS)
//· · · · · · · |-RuleRef(SP)
//· · · · · · · |-IgnoreCaseLiteral(None, WITH)
//· · · · · · |-Sequence(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · · |-IgnoreCaseLiteral(None, CONTAINS)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-RuleRef(PropertyOrLabelsExpression)
//· · · · |-Sequence(None)
//· · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, IS)
//· · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, NULL)
//· · · · |-Sequence(None)
//· · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, IS)
//· · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, NOT)
//· · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, NULL)
//  */
//case class YieldItem(procedureResultFieldOpt: Option[ProcedureResultField], variable: Variable) extends CypherAst
///**
//Grammar expression:
//|-Rule(YieldItem, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(ProcedureResultField)
//· · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, AS)
//· · · · |-RuleRef(SP)
//· · |-RuleRef(Variable)
//  */
//case class PowerOfExpression(unaryAddOrSubtractExpressions: List[UnaryAddOrSubtractExpression]) extends CypherAst
///**
//Grammar expression:
//|-Rule(PowerOfExpression, Some(CypherAst), true, false)
//· |-RepeatWithSeparator(1, None, Some(unaryAddOrSubtractExpressions))
//· · |-RuleRef(UnaryAddOrSubtractExpression)
//· · |-Sequence(None)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ^)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//  */
//abstract class SetItem extends Set
///**
//Grammar expression:
//|-Rule(SetItem, Some(Set), true, false)
//· |-Either(None)
//· · |-Sequence(None)
//· · · |-RuleRef(PropertyExpression)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, =)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(Expression)
//· · |-Sequence(None)
//· · · |-RuleRef(Variable)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, =)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(Expression)
//· · |-Sequence(None)
//· · · |-RuleRef(Variable)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, +=)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(Expression)
//· · |-Sequence(None)
//· · · |-RuleRef(Variable)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(NodeLabels)
//  */
//case class SetItemImpl57(()) extends SetItem
//case class SetItemImpl14(()) extends SetItem
//case class SetItemImpl49(()) extends SetItem
//case class SetItemImpl87(()) extends SetItem
//case class ProcedureName(namespace: Namespace, symbolicName: SymbolicName) extends CypherAst
///**
//Grammar expression:
//|-Rule(ProcedureName, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(Namespace)
//· · |-RuleRef(SymbolicName)
//  */
//abstract class RemoveItem extends Remove
///**
//Grammar expression:
//|-Rule(RemoveItem, Some(Remove), true, false)
//· |-Either(None)
//· · |-Sequence(None)
//· · · |-RuleRef(Variable)
//· · · |-RuleRef(NodeLabels)
//· · |-RuleRef(PropertyExpression)
//  */
//case class RemoveItemImpl75(()) extends RemoveItem
//abstract class Query extends CypherAst
///**
//Grammar expression:
//|-Rule(Query, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-RuleRef(RegularQuery)
//· · |-RuleRef(StandaloneCall)
//  */
//abstract class MergeAction extends CypherAst
///**
//Grammar expression:
//|-Rule(MergeAction, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, ON)
//· · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, MATCH)
//· · · |-RuleRef(SP)
//· · · |-RuleRef(Set)
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, ON)
//· · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, CREATE)
//· · · |-RuleRef(SP)
//· · · |-RuleRef(Set)
//  */
//case class ComparisonExpression(addOrSubtractExpression: AddOrSubtractExpression, partialComparisonExpressions: List[PartialComparisonExpression]) extends Expression
///**
//Grammar expression:
//|-Rule(ComparisonExpression, Some(Expression), true, false)
//· |-Sequence(None)
//· · |-RuleRef(AddOrSubtractExpression)
//· · |-Repeat(0, None, None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(PartialComparisonExpression)
//  */
//case class LabelName(schemaName: SchemaName) extends NodeLabels
///**
//Grammar expression:
//|-Rule(LabelName, Some(NodeLabels), true, false)
//· |-RuleRef(SchemaName)
//  */
//case class ReadUpdateEnd(readingClause: ReadingClause, readingClauses: List[ReadingClause], updatingClauses: List[UpdatingClause], returnOpt: Option[Return]) extends CypherAst
///**
//Grammar expression:
//|-Rule(ReadUpdateEnd, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(ReadingClause)
//· · |-Repeat(0, None, None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(ReadingClause)
//· · |-Repeat(1, None, None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(UpdatingClause)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Return)
//  */
//case class ImplicitProcedureInvocation(procedureName: ProcedureName) extends CypherAst
///**
//Grammar expression:
//|-Rule(ImplicitProcedureInvocation, Some(CypherAst), false, false)
//· |-RuleRef(ProcedureName)
//  */
//case class Where(expression: Expression) extends CypherAst
///**
//Grammar expression:
//|-Rule(Where, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, WHERE)
//· · |-RuleRef(SP)
//· · |-RuleRef(Expression)
//  */
//case class MultiplyDivideModuloExpression(powerOfExpression: PowerOfExpression, generatedAbstractClass8s: List[GeneratedAbstractClass8]) extends CypherAst
///**
//Grammar expression:
//|-Rule(MultiplyDivideModuloExpression, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(PowerOfExpression)
//· · |-Repeat(0, None, None)
//· · · |-Either(None)
//· · · · |-Sequence(None)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, *)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-RuleRef(PowerOfExpression)
//· · · · |-Sequence(None)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, /)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-RuleRef(PowerOfExpression)
//· · · · |-Sequence(None)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, %)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-RuleRef(PowerOfExpression)
//  */
//abstract class ReturnItem extends CypherAst
///**
//Grammar expression:
//|-Rule(ReturnItem, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-Sequence(None)
//· · · |-RuleRef(Expression)
//· · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, AS)
//· · · |-RuleRef(SP)
//· · · |-RuleRef(Variable)
//· · |-RuleRef(Expression)
//  */
//case class ReturnItemImpl87(()) extends ReturnItem
//case class UpdatingEnd(updatingStartClause: UpdatingStartClause, updatingClauses: List[UpdatingClause], returnOpt: Option[Return]) extends CypherAst
///**
//Grammar expression:
//|-Rule(UpdatingEnd, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(UpdatingStartClause)
//· · |-Repeat(0, None, None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(UpdatingClause)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Return)
//  */
//case class Delete(expressions: List[Expression]) extends CypherAst
///**
//Grammar expression:
//|-Rule(Delete, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-IgnoreCaseLiteral(None, DETACH)
//· · · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, DELETE)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RepeatWithSeparator(1, None, Some(expressions))
//· · · |-RuleRef(Expression)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, ,)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//  */
//case class RangeLiteral(integerLiteralOpt: Option[IntegerLiteral], integerLiteralOpt: Option[IntegerLiteral]) extends CypherAst
///**
//Grammar expression:
//|-Rule(RangeLiteral, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, *)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(IntegerLiteral)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-IgnoreCaseLiteral(None, ..)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-Optional(None)
//· · · · · |-Sequence(None)
//· · · · · · |-RuleRef(IntegerLiteral)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//  */
//case class UnaryAddOrSubtractExpression(generatedAbstractClass57s: List[GeneratedAbstractClass57], stringListNullOperatorExpression: StringListNullOperatorExpression) extends PowerOfExpression
///**
//Grammar expression:
//|-Rule(UnaryAddOrSubtractExpression, Some(PowerOfExpression), true, false)
//· |-Sequence(None)
//· · |-Repeat(0, None, None)
//· · · |-Sequence(None)
//· · · · |-Either(None)
//· · · · · |-IgnoreCaseLiteral(None, +)
//· · · · · |-IgnoreCaseLiteral(None, -)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-RuleRef(StringListNullOperatorExpression)
//  */
//case class RelationshipsPattern(nodePattern: NodePattern, patternElementChains: List[PatternElementChain]) extends CypherAst
///**
//Grammar expression:
//|-Rule(RelationshipsPattern, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(NodePattern)
//· · |-Repeat(1, None, None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(PatternElementChain)
//  */
//case class AndExpression(notExpressions: List[NotExpression]) extends Expression
///**
//Grammar expression:
//|-Rule(AndExpression, Some(Expression), true, false)
//· |-RepeatWithSeparator(1, None, Some(notExpressions))
//· · |-RuleRef(NotExpression)
//· · |-Sequence(None)
//· · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, AND)
//· · · |-RuleRef(SP)
//  */
//abstract class PatternPart extends CypherAst
///**
//Grammar expression:
//|-Rule(PatternPart, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-Sequence(None)
//· · · |-RuleRef(Variable)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, =)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(AnonymousPatternPart)
//· · |-RuleRef(AnonymousPatternPart)
//  */
//case class PatternPartImpl35(()) extends PatternPart
//abstract class IntegerLiteral extends CypherAst
///**
//Grammar expression:
//|-Rule(IntegerLiteral, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-RuleRef(HexInteger)
//· · |-RuleRef(OctalInteger)
//· · |-RuleRef(DecimalInteger)
//  */
//abstract class Statement extends Cypher
///**
//Grammar expression:
//|-Rule(Statement, Some(Cypher), true, false)
//· |-Either(None)
//· · |-RuleRef(Query)
//  */
//case class NotExpression(comparisonExpression: ComparisonExpression) extends Expression
///**
//Grammar expression:
//|-Rule(NotExpression, Some(Expression), true, false)
//· |-Sequence(None)
//· · |-Repeat(0, None, None)
//· · · |-Sequence(None)
//· · · · |-IgnoreCaseLiteral(None, NOT)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-RuleRef(ComparisonExpression)
//  */
//case class ReadPart(readingClauses: List[ReadingClause]) extends CypherAst
///**
//Grammar expression:
//|-Rule(ReadPart, Some(CypherAst), true, false)
//· |-Repeat(0, None, None)
//· · |-Sequence(None)
//· · · |-RuleRef(ReadingClause)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//  */
//case class LeftArrowHead(leftArrowHead: String) extends CypherAst
///**
//Grammar expression:
//|-Rule(LeftArrowHead, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-IgnoreCaseLiteral(None, <)
//· · |-IgnoreCaseLiteral(None, ⟨)
//· · |-IgnoreCaseLiteral(None, 〈)
//· · |-IgnoreCaseLiteral(None, ﹤)
//· · |-IgnoreCaseLiteral(None, ＜)
//  */
//abstract class ReadingClause extends CypherAst
///**
//Grammar expression:
//|-Rule(ReadingClause, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-RuleRef(Match)
//· · |-RuleRef(Unwind)
//· · |-RuleRef(InQueryCall)
//  */
//case class ProcedureResultField(symbolicName: SymbolicName) extends CypherAst
///**
//Grammar expression:
//|-Rule(ProcedureResultField, Some(CypherAst), true, false)
//· |-RuleRef(SymbolicName)
//  */
//abstract class DoubleLiteral extends CypherAst
///**
//Grammar expression:
//|-Rule(DoubleLiteral, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-RuleRef(ExponentDecimalReal)
//· · |-RuleRef(RegularDecimalReal)
//  */
//case class RelTypeName(schemaName: SchemaName) extends RelationshipTypes
///**
//Grammar expression:
//|-Rule(RelTypeName, Some(RelationshipTypes), true, false)
//· |-RuleRef(SchemaName)
//  */
//case class Namespace(symbolicNames: List[SymbolicName]) extends CypherAst
///**
//Grammar expression:
//|-Rule(Namespace, Some(CypherAst), true, false)
//· |-Repeat(0, None, None)
//· · |-Sequence(None)
//· · · |-RuleRef(SymbolicName)
//· · · |-IgnoreCaseLiteral(None, .)
//  */
//case class ListLiteral(generatedAbstractClass15Opt: Option[GeneratedAbstractClass15]) extends CypherAst
///**
//Grammar expression:
//|-Rule(ListLiteral, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, [)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(Expression)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-Repeat(0, None, None)
//· · · · · |-Sequence(None)
//· · · · · · |-IgnoreCaseLiteral(None, ,)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · |-RuleRef(Expression)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, ])
//  */
//abstract class SchemaName extends CypherAst
///**
//Grammar expression:
//|-Rule(SchemaName, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-RuleRef(SymbolicName)
//· · |-RuleRef(ReservedWord)
//  */
//case class MapLiteral(generatedAbstractClass90Opt: Option[GeneratedAbstractClass90]) extends CypherAst
///**
//Grammar expression:
//|-Rule(MapLiteral, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, {)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(PropertyKeyName)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, :)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Expression)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-Repeat(0, None, None)
//· · · · · |-Sequence(None)
//· · · · · · |-IgnoreCaseLiteral(None, ,)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · |-RuleRef(PropertyKeyName)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · |-IgnoreCaseLiteral(None, :)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · |-RuleRef(Expression)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, })
//  */
//case class InQueryCall(explicitProcedureInvocation: ExplicitProcedureInvocation, yieldItemsOpt: Option[YieldItems]) extends CypherAst
///**
//Grammar expression:
//|-Rule(InQueryCall, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, CALL)
//· · |-RuleRef(SP)
//· · |-RuleRef(ExplicitProcedureInvocation)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, YIELD)
//· · · · |-RuleRef(SP)
//· · · · |-RuleRef(YieldItems)
//  */
//abstract class YieldItems extends CypherAst
///**
//Grammar expression:
//|-Rule(YieldItems, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-RepeatWithSeparator(1, None, Some(yieldItems))
//· · · |-RuleRef(YieldItem)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, ,)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, -)
//  */
//case object - extends YieldItems
//case class FunctionInvocation(functionName: FunctionName, generatedAbstractClass15Opt: Option[GeneratedAbstractClass15]) extends CypherAst
///**
//Grammar expression:
//|-Rule(FunctionInvocation, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-RuleRef(FunctionName)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, ()
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-IgnoreCaseLiteral(None, DISTINCT)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(Expression)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-Repeat(0, None, None)
//· · · · · |-Sequence(None)
//· · · · · · |-IgnoreCaseLiteral(None, ,)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · |-RuleRef(Expression)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, ))
//  */
//case class XorExpression(andExpressions: List[AndExpression]) extends Expression
///**
//Grammar expression:
//|-Rule(XorExpression, Some(Expression), true, false)
//· |-RepeatWithSeparator(1, None, Some(andExpressions))
//· · |-RuleRef(AndExpression)
//· · |-Sequence(None)
//· · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, XOR)
//· · · |-RuleRef(SP)
//  */
//case class ReturnBody(returnItems: ReturnItems, orderOpt: Option[Order], skipOpt: Option[Skip], limitOpt: Option[Limit]) extends CypherAst
///**
//Grammar expression:
//|-Rule(ReturnBody, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-RuleRef(ReturnItems)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(SP)
//· · · · |-RuleRef(Order)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(SP)
//· · · · |-RuleRef(Skip)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(SP)
//· · · · |-RuleRef(Limit)
//  */
//case class Dash(dash: String) extends CypherAst
///**
//Grammar expression:
//|-Rule(Dash, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-IgnoreCaseLiteral(None, -)
//· · |-IgnoreCaseLiteral(None, ­)
//· · |-IgnoreCaseLiteral(None, ‐)
//· · |-IgnoreCaseLiteral(None, ‑)
//· · |-IgnoreCaseLiteral(None, ‒)
//· · |-IgnoreCaseLiteral(None, –)
//· · |-IgnoreCaseLiteral(None, —)
//· · |-IgnoreCaseLiteral(None, ―)
//· · |-IgnoreCaseLiteral(None, −)
//· · |-IgnoreCaseLiteral(None, ﹘)
//· · |-IgnoreCaseLiteral(None, ﹣)
//· · |-IgnoreCaseLiteral(None, －)
//  */
//case class Order(sortItem: SortItem, sortItems: List[SortItem]) extends CypherAst
///**
//Grammar expression:
//|-Rule(Order, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, ORDER)
//· · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, BY)
//· · |-RuleRef(SP)
//· · |-RuleRef(SortItem)
//· · |-Repeat(0, None, None)
//· · · |-Sequence(None)
//· · · · |-IgnoreCaseLiteral(None, ,)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(SortItem)
//  */
//abstract class PartialComparisonExpression extends CypherAst
///**
//Grammar expression:
//|-Rule(PartialComparisonExpression, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-Sequence(None)
//· · · |-StringLiteral(=)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(AddOrSubtractExpression)
//· · |-Sequence(None)
//· · · |-StringLiteral(<>)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(AddOrSubtractExpression)
//· · |-Sequence(None)
//· · · |-StringLiteral(<)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(AddOrSubtractExpression)
//· · |-Sequence(None)
//· · · |-StringLiteral(>)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(AddOrSubtractExpression)
//· · |-Sequence(None)
//· · · |-StringLiteral(<=)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(AddOrSubtractExpression)
//· · |-Sequence(None)
//· · · |-StringLiteral(>=)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(AddOrSubtractExpression)
//  */
//case class Unwind(expression: Expression, variable: Variable) extends CypherAst
///**
//Grammar expression:
//|-Rule(Unwind, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, UNWIND)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(Expression)
//· · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, AS)
//· · |-RuleRef(SP)
//· · |-RuleRef(Variable)
//  */
//abstract class Union extends CypherAst
///**
//Grammar expression:
//|-Rule(Union, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, UNION)
//· · · |-RuleRef(SP)
//· · · |-IgnoreCaseLiteral(None, ALL)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(SingleQuery)
//· · |-Sequence(None)
//· · · |-IgnoreCaseLiteral(None, UNION)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//· · · |-RuleRef(SingleQuery)
//  */
//case class IdInColl(variable: Variable, expression: Expression) extends CypherAst
///**
//Grammar expression:
//|-Rule(IdInColl, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(Variable)
//· · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, IN)
//· · |-RuleRef(SP)
//· · |-RuleRef(Expression)
//  */
//case class StandaloneCall(generatedAbstractClass32: GeneratedAbstractClass32, yieldItemsOpt: Option[YieldItems]) extends CypherAst
///**
//Grammar expression:
//|-Rule(StandaloneCall, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, CALL)
//· · |-RuleRef(SP)
//· · |-Either(None)
//· · · |-RuleRef(ExplicitProcedureInvocation)
//· · · |-RuleRef(ImplicitProcedureInvocation)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, YIELD)
//· · · · |-RuleRef(SP)
//· · · · |-RuleRef(YieldItems)
//  */
//case class CaseExpression(generatedAbstractClass57: GeneratedAbstractClass57, expressionOpt: Option[Expression]) extends CypherAst
///**
//Grammar expression:
//|-Rule(CaseExpression, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-Either(None)
//· · · |-Sequence(None)
//· · · · |-IgnoreCaseLiteral(None, CASE)
//· · · · |-Repeat(1, None, None)
//· · · · · |-Sequence(None)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · |-RuleRef(CaseAlternatives)
//· · · |-Sequence(None)
//· · · · |-IgnoreCaseLiteral(None, CASE)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Expression)
//· · · · |-Repeat(1, None, None)
//· · · · · |-Sequence(None)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · |-RuleRef(CaseAlternatives)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, ELSE)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Expression)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, END)
//  */
//case class Match(pattern: Pattern, whereOpt: Option[Where]) extends CypherAst
///**
//Grammar expression:
//|-Rule(Match, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-IgnoreCaseLiteral(None, OPTIONAL)
//· · · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, MATCH)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(Pattern)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Where)
//  */
//case class AddOrSubtractExpression(multiplyDivideModuloExpression: MultiplyDivideModuloExpression, generatedAbstractClass50s: List[GeneratedAbstractClass50]) extends CypherAst
///**
//Grammar expression:
//|-Rule(AddOrSubtractExpression, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(MultiplyDivideModuloExpression)
//· · |-Repeat(0, None, None)
//· · · |-Either(None)
//· · · · |-Sequence(None)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, +)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-RuleRef(MultiplyDivideModuloExpression)
//· · · · |-Sequence(None)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-IgnoreCaseLiteral(None, -)
//· · · · · |-Optional(None)
//· · · · · · |-RuleRef(SP)
//· · · · · |-RuleRef(MultiplyDivideModuloExpression)
//  */
//case class ExplicitProcedureInvocation(procedureName: ProcedureName, generatedAbstractClass15Opt: Option[GeneratedAbstractClass15]) extends CypherAst
///**
//Grammar expression:
//|-Rule(ExplicitProcedureInvocation, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-RuleRef(ProcedureName)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, ()
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(Expression)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-Repeat(0, None, None)
//· · · · · |-Sequence(None)
//· · · · · · |-IgnoreCaseLiteral(None, ,)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · · · · · |-RuleRef(Expression)
//· · · · · · |-Optional(None)
//· · · · · · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, ))
//  */
//case class Create(pattern: Pattern) extends CypherAst
///**
//Grammar expression:
//|-Rule(Create, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, CREATE)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(Pattern)
//  */
//abstract class UpdatingClause extends CypherAst
///**
//Grammar expression:
//|-Rule(UpdatingClause, Some(CypherAst), true, false)
//· |-Either(None)
//· · |-RuleRef(Create)
//· · |-RuleRef(Merge)
//· · |-RuleRef(Delete)
//· · |-RuleRef(Set)
//· · |-RuleRef(Remove)
//  */
//case class BooleanLiteral(booleanLiteral: String) extends CypherAst
///**
//Grammar expression:
//|-Rule(BooleanLiteral, Some(CypherAst), false, false)
//· |-Either(None)
//· · |-IgnoreCaseLiteral(None, TRUE)
//· · |-IgnoreCaseLiteral(None, FALSE)
//  */
//case class UpdatingPart(updatingClauses: List[UpdatingClause]) extends CypherAst
///**
//Grammar expression:
//|-Rule(UpdatingPart, Some(CypherAst), true, false)
//· |-Repeat(0, None, None)
//· · |-Sequence(None)
//· · · |-RuleRef(UpdatingClause)
//· · · |-Optional(None)
//· · · · |-RuleRef(SP)
//  */
//case class Skip(expression: Expression) extends CypherAst
///**
//Grammar expression:
//|-Rule(Skip, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, SKIP)
//· · |-RuleRef(SP)
//· · |-RuleRef(Expression)
//  */
//case class ListComprehension(filterExpression: FilterExpression, expressionOpt: Option[Expression]) extends CypherAst
///**
//Grammar expression:
//|-Rule(ListComprehension, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, [)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(FilterExpression)
//· · |-Optional(None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-IgnoreCaseLiteral(None, |)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Expression)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-IgnoreCaseLiteral(None, ])
//  */
//case class Merge(patternPart: PatternPart, mergeActions: List[MergeAction]) extends CypherAst
///**
//Grammar expression:
//|-Rule(Merge, Some(CypherAst), false, false)
//· |-Sequence(None)
//· · |-IgnoreCaseLiteral(None, MERGE)
//· · |-Optional(None)
//· · · |-RuleRef(SP)
//· · |-RuleRef(PatternPart)
//· · |-Repeat(0, None, None)
//· · · |-Sequence(None)
//· · · · |-RuleRef(SP)
//· · · · |-RuleRef(MergeAction)
//  */
//case class RegularQuery(singleQuery: SingleQuery, unions: List[Union]) extends CypherAst
///**
//Grammar expression:
//|-Rule(RegularQuery, Some(CypherAst), true, false)
//· |-Sequence(None)
//· · |-RuleRef(SingleQuery)
//· · |-Repeat(0, None, None)
//· · · |-Sequence(None)
//· · · · |-Optional(None)
//· · · · · |-RuleRef(SP)
//· · · · |-RuleRef(Union)
//  */
//case class PropertyKeyName(schemaName: SchemaName) extends CypherAst
///**
//Grammar expression:
//|-Rule(PropertyKeyName, Some(CypherAst), true, false)
//· |-RuleRef(SchemaName)
//  */
