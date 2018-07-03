package antlr.visitor

import scala.reflect.ClassTag

object TypeMatchingUtilities {

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

}
