package org.opencypher.fastparse

import org.opencypher.grammar.CharacterSet.DefinitionVisitor.NamedSetVisitor
import org.opencypher.grammar.CharacterSet.ExclusionVisitor
import org.opencypher.grammar.Grammar.Term
import org.opencypher.grammar.{Alternatives, CharacterSet, NonTerminal, Optional, Production, Repetition, Sequence => OcSequence, TermVisitor, Literal => OcLiteral}

import scala.collection.JavaConverters._

object GrammarVisitors {
  implicit class RichProduction(p: Production) {
    def convert: Rule = {
      Rule(p.name, p.inline, p.lexer, p.definition.convert)
    }
  }

  implicit class RichTerm(t: Term) {
    def convert: TermExpr = {
      case class TermBuilder() extends TermVisitor[RuntimeException] {

        def result: TermExpr = {
          assert(_result != null)
          _result
        }

        private var _result: TermExpr = _

        override def visitAlternatives(alternatives: Alternatives): Unit = {
          _result = Either(alternatives.asScala.toList.map(_.convert))
        }

        override def visitEpsilon(): Unit = {}

        override def visitSequence(sequence: OcSequence): Unit = {
          _result = Sequence(sequence.asScala.toList.map(_.convert))
        }

        override def visitNonTerminal(nonTerminal: NonTerminal): Unit = {
          _result = RuleRef(nonTerminal.productionName)
        }

        override def visitOptional(optional: Optional): Unit = {
          _result = Maybe(optional.term.convert)
        }

        override def visitCharacters(characters: CharacterSet): Unit = {
          _result = characters.convert
        }

        override def visitLiteral(literal: OcLiteral): Unit = {
          _result = literal.convert
        }

        override def visitRepetition(repetition: Repetition): Unit = {
          val maybeMax = if (repetition.limited) Some(repetition.maxTimes) else None
          _result = Rep(repetition.term.convert, repetition.minTimes, maybeMax)
        }
      }

      // Hide mutable visitor pattern
      val builder = TermBuilder()
      t.accept(builder)
      builder.result
    }

    implicit class RichCharacterSet(cs: CharacterSet) {
      def convert: TermExpr = {
        case class CharacterSetBuilder() extends NamedSetVisitor[RuntimeException] with ExclusionVisitor[RuntimeException] {

          def result: Fragment = {
            Fragment(codePoints -- exclusions, namedInclusions, namedExclusions)
          }

          var codePoints = Set.empty[Int]
          var exclusions = Set.empty[Int]
          var namedInclusions: Set[String] = Set.empty
          var namedExclusions: Set[String] = Set.empty

          override def visitCodePoint(cp: Int): Unit = codePoints += cp

          override def excludeCodePoint(cp: Int): Unit = exclusions -= cp

          override def visitSet(name: String): ExclusionVisitor[RuntimeException] = {
            namedInclusions += name
            this
          }

          override def excludeSet(name: String): Unit = {
            namedExclusions += name
          }
        }

        // Hide mutable visitor pattern
        val builder = CharacterSetBuilder()
        cs.accept(builder)
        builder.result
      }
    }

  }

  implicit class RichLiteral(l: OcLiteral) {
    def convert: Literal = {
      if (l.caseSensitive) StringLiteral(l.toString) else IgnoreCaseLiteral(l.toString)
    }
  }
}

