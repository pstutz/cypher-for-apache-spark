package org.opencypher.fastparse

import scala.language.higherKinds


object AstGenerator extends App {

  val rules: Map[String, Rule] = CypherGrammar.parserRules()

  rules.values.foreach(_.show())

}
