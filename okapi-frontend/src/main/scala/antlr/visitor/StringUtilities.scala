package antlr.visitor

object StringUtilities {

  implicit class StringAsParam(s: String) {

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

}
