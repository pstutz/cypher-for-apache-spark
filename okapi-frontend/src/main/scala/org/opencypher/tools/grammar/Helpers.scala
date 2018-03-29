package org.opencypher.tools.grammar

import org.opencypher.grammar.CharacterSet.DefinitionVisitor.NamedSetVisitor
import org.opencypher.grammar.CharacterSet.ExclusionVisitor
import org.opencypher.grammar.Grammar.Term
import org.opencypher.grammar._

import scala.language.higherKinds
import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.List
import scala.collection.mutable
import scala.reflect.ClassTag

object Helpers {

  implicit class RichString(s: String) {

    def letters: String = s.filter(_.isLetter)

    def isUpper: Boolean = s.toCharArray.forall(_.isUpper)

    def isKeyword: Boolean = s.isUpper && s.toCharArray.forall(_.isLetter)

    def firstCharToLowerCase: String = {
      if (s.length == 0) {
        s
      } else {
        val c = s.toCharArray
        c(0) = Character.toLowerCase(c(0))
        new String(c)
      }
    }

    def firstCharToUpperCase: String = {
      if (s.length == 0) {
        s
      } else {
        val c = s.toCharArray
        c(0) = Character.toUpperCase(c(0))
        new String(c)
      }
    }

    def asParamName: String = {
      if (isUpper) {
        s.toLowerCase
      } else {
        firstCharToLowerCase
      }
    }
  }


  implicit class RichList[I](val l: List[I]) extends AnyVal {
    def hasA[C: ClassTag]: Boolean = {
      l.collect { case c: C => c }.nonEmpty
    }

    def have[C: ClassTag](f: C => Boolean): Boolean = {
      are[C] && as[C].forall(f)
    }

    def are[C: ClassTag]: Boolean = {
      l.forall {
        case _: C => true
        case _ => false
      }
    }

    def mapOnly[C: ClassTag](f: C => I): List[I] = {
      l.map {
        case c: C => f(c)
        case other => other
      }
    }

    def as[C]: List[C] = {
      l.asInstanceOf[List[C]]
    }
  }

  implicit class RichArray[I: ClassTag](a: Array[I]) {
    def hasA[C: ClassTag]: Boolean = {
      a.collect { case c: C => c }.nonEmpty
    }

    def have[C: ClassTag](f: C => Boolean): Boolean = {
      are[C] && as[C].forall(f)
    }

    def are[C: ClassTag]: Boolean = {
      a.forall {
        case _: C => true
        case _ => false
      }
    }

    def mapOnly[C: ClassTag](f: C => I): Array[I] = {
      a.map {
        case c: C => f(c)
        case other => other
      }.toArray
    }

    def as[C]: Array[C] = {
      a.asInstanceOf[Array[C]]
    }
  }

  implicit class RichProduction(p: Production) {
    def convert: Rule = {
      Rule(p.name, None, p.inline, p.lexer, p.definition.convert)
    }
  }

  implicit class RichTerm(t: Term) {
    def convert: TermExpr = {
      case class TermBuilder() extends TermVisitor[RuntimeException] {

        def result = {
          assert(_result != null)
          _result
        }

        private var _result: TermExpr = _

        override def visitAlternatives(alternatives: Alternatives): Unit = {
          _result = Either(alternatives.asScala.toList.map(_.convert))
        }

        override def visitEpsilon(): Unit = {}

        override def visitSequence(sequence: org.opencypher.grammar.Sequence): Unit = {
          _result = Sequence(sequence.asScala.toList.map(_.convert))
        }

        override def visitNonTerminal(nonTerminal: NonTerminal): Unit = {
          _result = RuleRef(nonTerminal.productionName)
        }

        override def visitOptional(optional: org.opencypher.grammar.Optional): Unit = {
          _result = Optional(optional.term.convert, None)
        }

        override def visitCharacters(characters: CharacterSet): Unit = {
          _result = characters.convert
        }

        override def visitLiteral(literal: org.opencypher.grammar.Literal): Unit = {
          _result = literal.convert
        }

        override def visitRepetition(repetition: Repetition): Unit = {
          val max = if (repetition.limited) Some(repetition.maxTimes) else None
          _result = Repeat(repetition.term.convert, repetition.minTimes, max, None)
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

          def result = {
            Fragment(Option(cs.name), codePoints -- exclusions, namedInclusions, namedExclusions)
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

  implicit class RichLiteral(l: org.opencypher.grammar.Literal) {
    def convert: Literal = {
      if (l.caseSensitive) StringLiteral(l.toString) else IgnoreCaseLiteral(None, l.toString)
    }
  }

}
