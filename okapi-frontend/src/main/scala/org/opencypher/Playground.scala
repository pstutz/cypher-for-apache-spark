package org.opencypher

import org.opencypher.okapi.trees.AbstractTreeNode

abstract class CypherExpression extends AbstractTreeNode[CypherExpression]
//
//abstract class Properties extends CypherExpression
//case class Set(setItems: List[SetItem]) extends CypherExpression
//case class RelationshipDetail(option: Option[List[Variable]], option: Option[List[RelationshipTypes]], option: Option[RangeLiteral], option: Option[List[Properties]]) extends RelationshipPattern
//abstract class Atom extends CypherExpression
//case class Return(returnBody: ReturnBody) extends CypherExpression
//case class CaseAlternatives(expression: Expression) extends CypherExpression
//case class OrExpression(xorExpression: XorExpression, list: List[List[XorExpression]]) extends Expression
//abstract class RelationshipPattern extends CypherExpression
//case class With(returnBody: ReturnBody, option: Option[List[Where]]) extends CypherExpression
//abstract class PatternElement extends CypherExpression
//case class PropertyExpression(atom: Atom, list: List[List[PropertyLookup]]) extends CypherExpression
//case class RelationshipTypes(relTypeName: RelTypeName, list: List[List[RelTypeName]]) extends CypherExpression
//case class Parameter(anonymousEither: AnonymousAbstractClass#61206) extends CypherExpression
//case class Pattern(patternPart: PatternPart, list: List[List[PatternPart]]) extends CypherExpression
//case class PropertyOrLabelsExpression(atom: Atom, list: List[List[PropertyLookup]], option: Option[List[NodeLabels]]) extends CypherExpression
//case class PatternElementChain(relationshipPattern: RelationshipPattern, nodePattern: NodePattern) extends CypherExpression
//case class ReadOnlyEnd(readPart: ReadPart, return: Return) extends SinglePartQuery
//abstract class SinglePartQuery extends CypherExpression
//case class NodePattern(option: Option[List[Variable]], option: Option[List[NodeLabels]], option: Option[List[Properties]]) extends CypherExpression
//case class Variable(symbolicName: SymbolicName) extends CypherExpression
//case class NodeLabel(labelName: LabelName) extends NodeLabels
//case class Cypher(statement: Statement) extends CypherExpression
//abstract class SymbolicName extends CypherExpression
//case class MultiPartQuery(anonymousEither: AnonymousAbstractClass#61466, with: With, list: List[GeneratedRule12], singlePartQuery: SinglePartQuery) extends SingleQuery
//abstract class ReservedWord extends SchemaName
//case class SortItem(expression: Expression, option: Option[List[AnonymousAbstractClass#62497]]) extends CypherExpression
//case class Limit(expression: Expression) extends CypherExpression
//case class ParenthesizedExpression(expression: Expression) extends Atom
//abstract class ReturnItems extends CypherExpression
//abstract class RightArrowHead extends RelationshipPattern
//case class Remove(removeItem: RemoveItem, list: List[List[RemoveItem]]) extends UpdatingClause
//abstract class Literal extends Atom
//abstract class FunctionName extends CypherExpression
//case class PropertyLookup(anonymousEither: AnonymousAbstractClass#62626) extends CypherExpression
//case class Expression(orExpression: OrExpression) extends CypherExpression
//case class NodeLabels(nodeLabels: List[NodeLabel]) extends CypherExpression
//abstract class NumberLiteral extends Literal
//case class FilterExpression(idInColl: IdInColl, option: Option[List[Where]]) extends CypherExpression
//case class PatternComprehension(option: Option[List[Variable]], relationshipsPattern: RelationshipsPattern, option: Option[List[Expression]], expression: Expression) extends Atom
//abstract class UpdatingStartClause extends CypherExpression
//abstract class AnonymousPatternPart extends PatternPart
//abstract class SingleQuery extends CypherExpression
//case class StringListNullOperatorExpression(propertyOrLabelsExpression: PropertyOrLabelsExpression, list: List[AnonymousAbstractClass#62948]) extends CypherExpression
//case class YieldItem(option: Option[List[ProcedureResultField]], variable: Variable) extends YieldItems
//case class PowerOfExpression(unaryAddOrSubtractExpression: UnaryAddOrSubtractExpression, list: List[List[UnaryAddOrSubtractExpression]]) extends CypherExpression
//abstract class SetItem extends Set
//case class ProcedureName(namespace: Namespace, symbolicName: SymbolicName) extends CypherExpression
//abstract class RemoveItem extends CypherExpression
//abstract class Query extends Statement
//abstract class MergeAction extends CypherExpression
//case class ComparisonExpression(addOrSubtractExpression: AddOrSubtractExpression, list: List[List[PartialComparisonExpression]]) extends NotExpression
//case class LabelName(schemaName: SchemaName) extends NodeLabel
//case class ReadUpdateEnd(readingClause: ReadingClause, list: List[List[ReadingClause]], list: List[List[UpdatingClause]], option: Option[List[Return]]) extends SinglePartQuery
//case class ImplicitProcedureInvocation(procedureName: ProcedureName) extends CypherExpression
//case class Where(expression: Expression) extends CypherExpression
//case class MultiplyDivideModuloExpression(powerOfExpression: PowerOfExpression, list: List[AnonymousAbstractClass#63291]) extends CypherExpression
//abstract class ReturnItem extends ReturnItems
//case class UpdatingEnd(updatingStartClause: UpdatingStartClause, list: List[List[UpdatingClause]], option: Option[List[Return]]) extends SinglePartQuery
//case class Delete(expression: Expression, list: List[List[Expression]]) extends UpdatingClause
//case class RangeLiteral(option: Option[List[IntegerLiteral]], option: Option[List[Option[List[IntegerLiteral]]]]) extends CypherExpression
//case class UnaryAddOrSubtractExpression(list: List[List[AnonymousAbstractClass#63678]], stringListNullOperatorExpression: StringListNullOperatorExpression) extends CypherExpression
//case class RelationshipsPattern(nodePattern: NodePattern, list: List[List[PatternElementChain]]) extends CypherExpression
//case class AndExpression(notExpression: NotExpression, list: List[List[NotExpression]]) extends CypherExpression
//abstract class PatternPart extends CypherExpression
//abstract class IntegerLiteral extends CypherExpression
//abstract class Statement extends Cypher
//case class NotExpression(comparisonExpression: ComparisonExpression) extends CypherExpression
//case class ReadPart(list: List[List[ReadingClause]]) extends CypherExpression
//abstract class LeftArrowHead extends RelationshipPattern
//abstract class ReadingClause extends CypherExpression
//case class ProcedureResultField(symbolicName: SymbolicName) extends CypherExpression
//abstract class DoubleLiteral extends NumberLiteral
//case class RelTypeName(schemaName: SchemaName) extends CypherExpression
//case class Namespace(list: List[List[SymbolicName]]) extends CypherExpression
//case class ListLiteral(option: Option[GeneratedRule13]) extends Literal
//abstract class SchemaName extends CypherExpression
//case class MapLiteral(option: Option[GeneratedRule15]) extends CypherExpression
//case class InQueryCall(explicitProcedureInvocation: ExplicitProcedureInvocation, option: Option[List[YieldItems]]) extends ReadingClause
//abstract class YieldItems extends CypherExpression
//case class FunctionInvocation(functionName: FunctionName, option: Option[GeneratedRule18]) extends Atom
//case class XorExpression(andExpression: AndExpression, list: List[List[AndExpression]]) extends CypherExpression
//case class ReturnBody(returnItems: ReturnItems, option: Option[List[Order]], option: Option[List[Skip]], option: Option[List[Limit]]) extends CypherExpression
//abstract class Dash extends RelationshipPattern
//case class Order(sortItem: SortItem, list: List[List[SortItem]]) extends CypherExpression
//abstract class PartialComparisonExpression extends CypherExpression
//case class Unwind(expression: Expression, variable: Variable) extends ReadingClause
//abstract class Union extends CypherExpression
//case class IdInColl(variable: Variable, expression: Expression) extends CypherExpression
//case class StandaloneCall(anonymousEither: AnonymousAbstractClass#65405, option: Option[List[YieldItems]]) extends Query
//case class CaseExpression(anonymousEither: AnonymousAbstractClass#65406, option: Option[List[Expression]]) extends Atom
//case class Match(pattern: Pattern, option: Option[List[Where]]) extends ReadingClause
//case class AddOrSubtractExpression(multiplyDivideModuloExpression: MultiplyDivideModuloExpression, list: List[AnonymousAbstractClass#65542]) extends CypherExpression
//case class ExplicitProcedureInvocation(procedureName: ProcedureName, option: Option[GeneratedRule19]) extends CypherExpression
//case class Create(pattern: Pattern) extends CypherExpression
//abstract class UpdatingClause extends CypherExpression
//abstract class BooleanLiteral extends Literal
//case class UpdatingPart(list: List[List[UpdatingClause]]) extends CypherExpression
//case class Skip(expression: Expression) extends CypherExpression
//case class ListComprehension(filterExpression: FilterExpression, option: Option[List[Expression]]) extends Atom
//case class Merge(patternPart: PatternPart, list: List[List[MergeAction]]) extends CypherExpression
//case class RegularQuery(singleQuery: SingleQuery, list: List[List[Union]]) extends Query
//case class PropertyKeyName(schemaName: SchemaName) extends CypherExpression
