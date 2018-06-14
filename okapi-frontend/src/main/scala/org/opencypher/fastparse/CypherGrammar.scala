package org.opencypher.fastparse

import java.io.File

import org.opencypher.fastparse.GrammarVisitors._
import org.opencypher.grammar.{Grammar, Production, ProductionVisitor}

object CypherGrammar {

  val oc = Grammar.OPENCYPHER_XML_NAMESPACE

  val cypherPath = new File("./okapi-frontend/grammar/cypher.xml").toPath

  def parserRules(): Map[String, Rule] = {
    val grammar: Grammar = Grammar.parseXML(cypherPath)
    var productions = Set.empty[Production]
    grammar.accept(new ProductionVisitor[Exception] {
      override def visitProduction(production: Production): Unit = {
        if (!production.legacy) productions += production
      }
    })
    val parsedRules: Map[String, Rule] = productions.map(p => p.name -> p.convert).toMap
    parsedRules
  }
}
