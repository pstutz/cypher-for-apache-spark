//package org.opencypher.tools.grammar
//
//import java.io.{ByteArrayOutputStream, OutputStream, Writer}
//import java.lang.Character.{charCount, isUpperCase, toUpperCase}
//import java.lang.String.format
//import java.nio.charset.StandardCharsets.UTF_8
//import java.util
//
//import org.opencypher.grammar.CharacterSet.DefinitionVisitor
//import org.opencypher.grammar.{CharacterSet, Grammar, NonTerminal, Production}
//import org.opencypher.tools.grammar
//import org.opencypher.tools.io._
//import org.opencypher.tools.io.OutputWriter
//
//
//class AnyCharacterExceptFormatter[grammar](val output: Output) extends
//  DefinitionVisitor.NamedSetVisitor[RuntimeException] with CharacterSet.ExclusionVisitor[RuntimeException] {
//  override def visitSet(name: String): CharacterSet.ExclusionVisitor[RuntimeException] = {
//    output.append("~[")
//    this
//  }
//
//  override def visitCodePoint(cp: Int): Unit = {
//    throw new UnsupportedOperationException
//  }
//
//  override def excludeCodePoint(cp: Int): Unit = {
//    codePoint(output, cp)
//  }
//
//  override def excludeRange(start: Int, end: Int): Unit = {
//    codePoint(output, start)
//    output.append('-')
//    codePoint(output, end)
//  }
//
//  override def excludeSet(name: String): Unit = {
//    throw new UnsupportedOperationException("ANY except a named set.")
//  }
//
//  override def close(): Unit = {
//    output.append("]")
//  }
//}
//
//object Fastparse {
//
//  def escapes(cp: Int): String = {
//    cp match {
//      case '\r' => "\\r"
//      case '\n' => "\\n"
//      case '\t' => "\\t"
//      case '\b' => "\\b"
//      case '\f' => "\\f"
//      case '\'' => "\\'"
//      case '\\' => "\\\\"
//      case _ => null
//    }
//
//
//    class SetFormatter[grammar](val output: Output) extends CharacterSet.DefinitionVisitor[RuntimeException] {
//      override def visitCodePoint(cp: Int): Unit = {
//        if (cp <= Character.MAX_VALUE) codePoint(output, cp)
//      }
//
//      override def visitRange(start: Int, end: Int): Unit = { // Antlr only supports unicode literals up to \uFFFF
//        if (end <= Character.MAX_VALUE) {
//          codePoint(output, start)
//          output.append('-')
//          codePoint(output, end)
//        }
//        else if (start <= Character.MAX_VALUE) {
//          codePoint(output, start)
//          output.append('-')
//          // truncate the range
//          codePoint(output, Character.MAX_VALUE)
//        }
//        // just skip larger values
//      }
//    }
//
//    def codePoint(output: Output, cp: Int): Unit = {
//      cp match {
//        case '\r' => output.append("\\r")
//        case '\n' => output.append("\\n")
//        case '\t' => output.append("\\t")
//        case '\b' => output.append("\\b")
//        case '\f' => output.append("\\f")
//        case '\\' => output.append("\\\\")
//        case '-' => output.append("\\-")
//        case ']' => output.append("\\]")
//        case _ =>
//          if (' ' <= cp && cp <= '~') output.appendCodePoint(cp)
//          else output.format("\\u%04X", cp)
//      }
//    }
//  }
//
//  class Fastparse(val output: Output) extends BnfWriter(output) {
//    final val fragmentRules = new util.HashMap[String, CharacterSet]
//    final val seenKeywords = new util.HashSet[String]
//    /*
//         * Mutable state -- this map keeps track of the keywords in the current production,
//         * and is emptied when the production is exited.
//         */ final val keywordsInProduction = new util.LinkedHashMap[String, String]
//
//    override def close(): Unit = {
//      /*
//              * This prints all the 'fragment' rules (rules that are used to construct lexer tokens,
//              * and the only type of rule that can use the character set syntax []) at the bottom of the grammar file.
//              */
//      import scala.collection.JavaConversions._
//      for (rule <- fragmentRules.entrySet) {
//        val set = rule.getValue
//        output.append("fragment ")
//        lexerRule(rule.getKey).append(" : ")
//        if (CharacterSet.ANY == set.name) set.accept(new AnyCharacterExceptFormatter(output))
//        else {
//          output.append('[')
//          set.accept(new Fastparse.SetFormatter(output))
//          output.append(']')
//        }
//        output.println(" ;\n")
//      }
//    }
//
//    override protected def productionCommentPrefix(): Unit = {
//      output.append("/**\n * ")
//    }
//
//    override protected def productionCommentLinePrefix(): Unit = {
//      output.append(" * ")
//    }
//
//    override protected def productionCommentSuffix(): Unit = {
//      output.println(" */")
//    }
//
//    var currentProduction = null
//    var nextLexerRule = 0
//
//    override protected def productionStart(p: Production): Unit = {
//      currentProduction = p.name
//      if (p.lexer) lexerRule(currentProduction).append(" : ")
//      else {
//        parserRule(currentProduction).append(" : ")
//        nextLexerRule = 0
//      }
//    }
//
//    override protected def productionEnd(): Unit = {
//      output.println(" ;").println
//      /*
//               * We print out lexer rules for all literal words mentioned in the production
//               */
//      import scala.collection.JavaConversions._
//      for (lexerRule <- keywordsInProduction.entrySet) {
//        val ruleName = lexerRule.getKey
//        // Except the ones we've already seen!
//        if (!seenKeywords.contains(ruleName)) {
//          seenKeywords.add(ruleName)
//          caseInsensitiveProductionStart(ruleName)
//          for (c <- lexerRule.getValue.toCharArray) {
//            groupWith('(', () => {
//              def foo() = {
//                literal(String.valueOf(c).toUpperCase)
//                alternativesSeparator()
//                literal(String.valueOf(c).toLowerCase)
//              }
//
//              foo()
//            }, ')')
//            sequenceSeparator()
//          }
//          output.println(" ;").println
//        }
//      }
//      keywordsInProduction.clear()
//      currentProduction = null
//    }
//
//    def addFragmentRule(characters: CharacterSet): Unit = {
//      val rule = currentProduction + "_" + {
//        nextLexerRule += 1;
//        nextLexerRule - 1
//      }
//      fragmentRules.put(rule, characters)
//      lexerRule(rule)
//    }
//
//    override protected def alternativesLinePrefix(altPrefix: Int): Unit = {
//      if (altPrefix > 0) {
//        output.println
//        while ( {
//          {
//            altPrefix -= 1;
//            altPrefix + 1
//          } > 0
//        }) output.append(' ')
//      }
//    }
//
//    override protected def alternativesSeparator(): Unit = {
//      output.append(" | ")
//    }
//
//    override protected def sequenceSeparator(): Unit = {
//      output.append(" ")
//    }
//
//    override protected def groupPrefix(): Unit = {
//      output.append("( ")
//    }
//
//    override protected def groupSuffix(): Unit = {
//      output.append(" )")
//    }
//
//    override protected def optionalPrefix = false
//
//    override protected def optionalSuffix(): Unit = {
//      output.append("?")
//    }
//
//    override protected def repeat(minTimes: Int, maxTimes: Integer, repeated: Runnable): Unit = {
//      if (maxTimes == null) if (minTimes == 0) {
//        groupWith('(', repeated, ')')
//        output.append("*")
//        return
//      }
//      else if (minTimes == 1) {
//        groupWith('(', repeated, ')')
//        output.append("+")
//        return
//      }
//      else if ((maxTimes eq 1) && minTimes == 0) {
//        groupWith('(', repeated, ')')
//        optionalSuffix()
//        return
//      }
//      else if (minTimes == maxTimes) {
//        groupWith('(', () => {
//          def foo() = {
//            var i = 0
//            while ( {
//              i < minTimes
//            }) {
//              if (i > 0) sequenceSeparator()
//              repeated.run()
//
//              {
//                i += 1;
//                i - 1
//              }
//            }
//          }
//
//          foo()
//        }, ')')
//        return
//      }
//      throw new UnsupportedOperationException(format("The Antlr formatter does not support minTimes=%d, maxTimes=%s", minTimes, maxTimes))
//    }
//
//    override protected def characterSet(characters: CharacterSet): Unit = {
//      val setName = characters.name
//      if (setName == null) addFragmentRule(characters)
//      else if (setName == CharacterSet.EOI) output.append("EOF")
//      else {
//        fragmentRules.put(setName, characters)
//        lexerRule(setName)
//      }
//    }
//
//    override protected def nonTerminal(nonTerminal: NonTerminal): Unit = {
//      if (nonTerminal.production.lexer) lexerRule(nonTerminal.productionName)
//      else parserRule(nonTerminal.productionName)
//    }
//
//    def parserRule(name: String) = output.append(prefix(name))
//
//    def lexerRule(name: String): Output = {
//      val cp = name.codePointAt(0)
//      if (!isUpperCase(cp)) {
//        if (name.codePoints.noneMatch(Character.isUpperCase)) return output.append(name.toUpperCase)
//        output.appendCodePoint(toUpperCase(cp)).append(name, charCount(cp), name.length)
//      }
//      else output.append(name)
//    }
//
//    override protected def prefix(s: String): String = grammar.Fastparse.PREFIX + s
//
//    override protected def literal(value: String): Unit = {
//      escapeAndEnclose(value)
//    }
//
//    def reserved(ruleName: String) = ruleName == "SKIP"
//
//    def inline(value: String): Unit = {
//      group(() => {
//        def foo() = {
//          var sep = ""
//          var start = 0
//          var i = 0
//          val end = value.length
//          var cp = 0
//          while ( {
//            i < end
//          }) {
//            cp = value.charAt(i)
//            if (Character.isLowerCase(cp) || Character.isUpperCase(cp) || Character.isTitleCase(cp)) {
//              if (start < i) {
//                output.append(sep)
//                sep = " "
//                escapeAndEnclose(value.substring(start, i))
//              }
//              output.append(sep)
//              sep = " "
//              start = i + Character.charCount(cp)
//              cp = Character.toUpperCase(cp)
//              val upper = String.valueOf(cp.toChar)
//              escapeAndEnclose(upper)
//              alternativesSeparator()
//              escapeAndEnclose(upper.toLowerCase)
//            }
//
//            i += Character.charCount(cp)
//          }
//          if (start < value.length) {
//            output.append(sep)
//            escapeAndEnclose(value.substring(start))
//          }
//        }
//
//        foo()
//      })
//    }
//
//    override protected def caseInsensitive(value: String): Unit = {
//      if (value.length == 1) inline(value)
//      else {
//        var lexerRule = value.toUpperCase
//        if (!Character.isLetter(value.codePointAt(0)) || reserved(lexerRule)) lexerRule = "L_" + lexerRule
//        output.append(lexerRule)
//        keywordsInProduction.put(lexerRule, value)
//      }
//    }
//
//    def escapeAndEnclose(value: String): Unit = {
//      output.append("'").escape(value, grammar.Fastparse.escapes).append("'")
//    }
//
//    override protected def caseInsensitiveProductionStart(name: String): Unit = {
//      currentProduction = name
//      lexerRule(currentProduction).append(" : ")
//      nextLexerRule = 0
//    }
//
//    override protected def epsilon(): Unit = {
//    }
//  }
