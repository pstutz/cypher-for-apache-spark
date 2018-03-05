//package org.opencypher.tools.grammar
//
//object CypherParserGenerator extends App {
//
//  val g =
//
//}


//  val productions = (allXml \\ "production").filter(p => !p.has("legacy"))
//
//  object Production {
//    def childToParameterString(c: Node): Option[String] = {
//      c match {
//        case elem: Elem => Some(elem.nameToString(new StringBuilder).toString)
//        case _: Text => None
//        case _ => None
//        //
//      }
//
//    }
//  }
//
//  case class Production(name: String, rhs: Seq[Node], inline: Boolean, lexer: Boolean) {
//
//    override def toString: String = {
//      s"$name(${rhs.flatMap(Production.childToParameterString(_))})"
//    }
//  }
//
//  var productionMap: Map[String, Production] = productions.map { node: Node =>
//    val name = node.attr("name").get
//    val inline = node.has("inline")
//    val lexer = node.has("lexer")
//    //    val legacy = node.has("legacy")
//    name -> Production(name, node.child, inline, lexer) //, legacy
//  }.toMap
//
//  println(productionMap.mkString("\n"))

//val root = productionMap(rootName)


//transformProduction(root).map(_.pretty).foreach(println)

//  def transformProduction(n: Node): Option[GrammarExpr] = {
//    println(n \ "@name")
//    if (n.has("legacy")) {
//      println(s"filtered legacy node $n")
//      None
//    } else {
//      if (n.label == "non-terminal") {
//        productionMap(n.attr("ref").get).flatMap(transformProduction)
//      } else {
//        val children = n.child.flatMap { c =>
//          if (c.has("lexer")) Literal(c.toString)
//          else transformProduction(c)
//        }
//        if (children.isEmpty) {
//          if (n.has("lexer")) Some(Literal(n.toString)) else println(s"none @ $n");
//          None
//        } else if (children.length == 1) {
//          Some(children.head)
//        } else {
//          Some(Sequence(children.toList))
//        }
//      }
//    }
//  }


//  val cypherInputStream = getClass.getClassLoader.getResourceAsStream(cypherPath)
//  val cypherResource = getClass.getClassLoader.getResource(cypherPath).getPath


//  val grammar: Grammar = Grammar.parseXML(Paths.get(cypherResource))

//  object Option extends Enumeration {
//    type Option = Value
//    val IGNORE_UNUSED_PRODUCTIONS, ALLOW_ROOTLESS = Value
//    private val option = nulldef
//    this (option: Root.ResolutionOption) {
//      this ()
//      this.option = option
//    }
//  }

//  val XML_NAMESPACE = "http://opencypher.org/grammar"
//  val SCOPE_XML_NAMESPACE = "http://opencypher.org/scope"
//  val GENERATOR_XML_NAMESPACE = "http://opencypher.org/stringgeneration"
//  val RAILROAD_XML_NAMESPACE = "http://opencypher.org/railroad"
//  val OPENCYPHER_XML_NAMESPACE = "http://opencypher.org/opencypher"
//
//  static Grammar parseXML(Path input, ParserOption...options
//  )


//  val pp = new PrettyPrinter(80, 2)
//  val basicGrammarXml = XML.load(getClass.getClassLoader.getResource("basic-grammar.xml"))
//  val cypherXml = XML.load(getClass.getClassLoader.getResource("cypher.xml"))
//
//  val allXml = basicGrammarXml ++ cypherXml
//
//  val productions = allXml \\ "production"
//  productions.foreach { node: Node =>
//    println(node.label + node.attributes)
//  }

//println(cypherXml)
//pp.format(cypherXml)


