package org.opencypher.caps.impl.inmemory

import java.net.URI

import org.opencypher.caps.api.graph.{CypherGraph, CypherResult, CypherSession, Plan}
import org.opencypher.caps.api.io.{GraphSource, PersistMode}
import org.opencypher.caps.api.record._
import org.opencypher.caps.api.schema.Schema
import org.opencypher.caps.api.types.{CTNode, CTRelationship}
import org.opencypher.caps.api.util.PrintOptions
import org.opencypher.caps.api.value.CypherValue
import org.opencypher.caps.impl.logical.{LogicalOperator, LogicalPlannerContext}
import org.opencypher.caps.impl.parse.CypherParser
import org.opencypher.caps.ir.api.IRExternalGraph
import org.opencypher.caps.ir.impl.{IRBuilder, IRBuilderContext}
import org.s1ck.gdl.GDLHandler

case class SimpleGraphSource(
    session: SimpleSession
) extends GraphSource {
  override type Session = this.type
  override type Graph = SimpleGraph

  override def sourceForGraphAt(uri: URI): Boolean = ???

  override def canonicalURI: URI = ???

  override def create: SimpleGraphSource.this.type = ???

  override def graph: SimpleGraphSource.this.type = ???

  override def store(graph: SimpleGraph, mode: PersistMode): SimpleGraph = ???

  override def delete(): Unit = ???

}

case class SimpleSession() extends CypherSession {
  override type Graph = SimpleGraph
  override type Session = SimpleSession
  override type Records = SimpleRecords
  override type Result = SimpleResult
  override type Data = Iterator[Map[String, CypherValue]]

  override val emptyGraph: Graph = SimpleGraph.empty(this)

  override def graphAt(uri: URI): SimpleGraph = ???

  override def cypher(graph: SimpleGraph, query: String, parameters: Map[String, CypherValue]): SimpleResult = {

    val ambientGraph = IRExternalGraph("___ambient", graph.schema, URI.create("session://ambient"))
    val (stmt, extractedLiterals, semState) = CypherParser.process(query)(CypherParser.defaultContext)

    val extractedParameters = extractedLiterals.mapValues(v => CypherValue(v))
    val allParameters = parameters ++ extractedParameters

    val ir = IRBuilder(stmt)(
      IRBuilderContext.initial(query, allParameters, semState, ambientGraph, _ => SimpleGraphSource(this)))

    val logicalPlannerContext = LogicalPlannerContext(graph.schema, Set.empty, ir.model.graphs.andThen(sourceAt))
    val logicalPlan = logicalPlanner(ir)(logicalPlannerContext)

    logStageProgress("Optimizing logical plan ...", false)
    val optimizedLogicalPlan = logicalOptimizer(logicalPlan)(logicalPlannerContext)
    logStageProgress("Done!")

    if (PrintLogicalPlan.get()) {
      println("Logical plan:")
      println(logicalPlan.pretty())
      println("Optimized logical plan:")
      println(optimizedLogicalPlan.pretty())
    }

    plan(graph, CAPSRecords.unit()(this), allParameters, optimizedLogicalPlan)
  }
}

case class SimpleRecords() extends CypherRecords {
  override type Data = Iterator[Map[String, CypherValue]]
  override type Records = SimpleRecords

  override def header: CypherRecordHeader = ???

  override def data: Data = ???

  override def contract[E <: EmbeddedEntity](entity: VerifiedEmbeddedEntity[E]): SimpleRecords.this.type = ???

  override def compact(implicit details: RetainedDetails): SimpleRecords.this.type = ???

  override def print(implicit options: PrintOptions): Unit = ???
}

case class SimpleResult(records: SimpleRecords) extends CypherResult {
  override type Graph = SimpleGraph
  override type Records = SimpleRecords

  override def graphs: Map[String, SimpleGraph] = ???

  override type LogicalPlan = LogicalOperator
  override type FlatPlan = LogicalOperator
  override type PhysicalPlan = LogicalOperator

  override def explain: Plan[LogicalOperator, LogicalOperator, LogicalOperator] = ???

  override def print(implicit options: PrintOptions): Unit = ???
}

case class SimpleGraph(impl: GDLHandler)(implicit val session: SimpleSession) extends CypherGraph {
  override type Graph = SimpleGraph
  override type Records = SimpleRecords
  override type Result = SimpleResult
  override type Session = SimpleSession

  override def schema: Schema = ???

  override def nodes(name: String, nodeCypherType: CTNode): SimpleRecords = ???

  override def relationships(name: String, relCypherType: CTRelationship): SimpleRecords = ???

  override def union(other: SimpleGraph): SimpleGraph = ???

  override protected def graph: SimpleGraph = this
}

object SimpleGraph {
  def empty(implicit session: SimpleSession) = SimpleGraph("")

  def apply(gdl: String)(implicit session: SimpleSession): SimpleGraph = {
    val impl = new GDLHandler.Builder()
      .disableDefaultVertexLabel()
      .buildFromString(gdl)
    SimpleGraph(impl)
  }
}
