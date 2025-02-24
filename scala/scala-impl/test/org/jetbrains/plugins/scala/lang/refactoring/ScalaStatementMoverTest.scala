package org.jetbrains.plugins.scala
package lang.refactoring

class ScalaStatementMoverTest extends StatementMoverTestBase {
  def testSingleLineMember(): Unit = {
    s"${|}def a".moveUpIsDisabled()
    s"${|}def a".moveDownIsDisabled()

    s"${|}def a\ndef b\n".moveUpIsDisabled()
    s"def a\n${|}def b\n" movedUpIs "def b\ndef a"

    s"${|}def a\ndef b" movedDownIs "def b\ndef a"
    s"def a\n${|}def b".moveDownIsDisabled()

    s"${|}def a\ndef b\ndef c".moveUpIsDisabled()
    s"def a\n${|}def b\ndef c" movedUpIs "def b\ndef a\ndef c"
    s"def a\ndef b\n${|}def c" movedUpIs "def a\ndef c\ndef b"

    s"${|}def a\ndef b\ndef c" movedDownIs "def b\ndef a\ndef c"
    s"def a\n${|}def b\ndef c" movedDownIs "def a\ndef c\ndef b"
    s"def a\ndef b\n${|}def c".moveDownIsDisabled()
  }

  def testCursorPositioning(): Unit = {
    s"${|}def a\ndef b" movedDownIs "def b\ndef a"
    s"def${|} a\ndef b" movedDownIs "def b\ndef a"
    s"def a${|}\ndef b" movedDownIs "def b\ndef a"
    s"def a {${|}}\ndef b {}" movedDownIs "def b {}\ndef a {}"
    s"def a {}${|}\ndef b {}" movedDownIs "def b {}\ndef a {}"
  }

  def testCursorLinePositioning(): Unit = {
    s"def a {\n${|}\n}\ndef b {\n\n}".moveDownIsDisabled()
  }

  def testLineSpace(): Unit = {
    s"def a\n\n${|}def b" movedUpIs "def b\n\ndef a"
    s"${|}def a\n\ndef b" movedDownIs "def b\n\ndef a"
  }

  def testExpressionAsSource(): Unit = {
    s"${|}v = 1\ndef a".moveDownIsDisabled()
  }

  def testExpressionAsTarget(): Unit = {
    s"${|}def a\nv = 1\ndef b" movedDownIs "v = 1\ndef a\ndef b"
  }

  def testSkipComment(): Unit = {
    s"${|}def a\n//comment\n\ndef b" movedDownIs "def b\n//comment\n\ndef a"
  }

  def testSourceComment(): Unit = {
    s"//source\n${|}def a\ndef b" movedDownIs "def b\n//source\ndef a"
    s"//source 1\n//source 2\n${|}def a\ndef b" movedDownIs "def b\n//source 1\n//source 2\ndef a"
    s"//foo\n\n//source\n${|}def a\ndef b" movedDownIs "//foo\n\ndef b\n//source\ndef a"
  }

  def testDestinationComment(): Unit = {
    s"//source\ndef a\n${|}def b" movedUpIs "def b\n//source\ndef a"
    s"//source 1\n//source 2\ndef a\n${|}def b" movedUpIs "def b\n//source 1\n//source 2\ndef a"
    s"//foo\n\n//source\ndef a\n${|}def b" movedUpIs "//foo\n\ndef b\n//source\ndef a"
  }

  def testMultipleLinesMember(): Unit = {
    s"def a {\n// method a\n}\n\n${|}def b {\n// method b\n}" movedUpIs "def b {\n  // method b\n}\n\ndef a {\n// method a\n}"
    s"${|}def a {\n// method a\n}\n\ndef b {\n// method b\n}" movedDownIs "def b {\n// method b\n}\n\ndef a {\n  // method a\n}"

    s"${|}def a\n\ndef b {\n// method b\n}" movedDownIs "def b {\n// method b\n}\n\ndef a"
    s"${|}def a {\n// method a\n}\n\ndef b" movedDownIs "def b\n\ndef a {\n  // method a\n}"
  }

  def testCaseClause(): Unit = {
    //    s"1 match {\n${|}case 1 =>\ncase 2 =>\ncase 3 =>\n}" moveUpIsDisabled;
    s"1 match {\n${|}case 1 =>\ncase 2 =>\ncase 3 =>\n}" movedDownIs "1 match {\ncase 2 =>\ncase 1 =>\ncase 3 =>\n}"

    s"1 match {\ncase 1 =>\n${|}case 2 =>\ncase 3 =>\n}" movedUpIs "1 match {\n  case 2 =>\n  case 1 =>\ncase 3 =>\n}"
    s"1 match {\ncase 1 =>\n${|}case 2 =>\ncase 3 =>\n}" movedDownIs "1 match {\ncase 1 =>\ncase 3 =>\ncase 2 =>\n}"

    s"1 match {\ncase 1 =>\ncase 2 =>\n${|}case 3 =>\n}" movedUpIs "1 match {\ncase 1 =>\ncase 3 =>\ncase 2 =>\n}"
    //    s"1 match {\ncase 1 =>\ncase 2 =>\n${|}case 3 =>\n}}" moveDownIsDisabled;
  }

  def testMultilineCaseClause(): Unit = {
    s"1 switch {\n${|}case 1 =>{\n//clause 1\n}\n\ncase 2 =>\n}}" movedDownIs "1 switch {\ncase 2 =>\n\ncase 1 =>{\n  //clause 1\n}\n}}"
    s"1 switch {\n${|}case 1 =>\n\ncase 2 =>{\n//clause 2\n}\n}}" movedDownIs "1 switch {\ncase 2 =>{\n//clause 2\n}\n\ncase 1 =>\n}}"
  }

  def testMoveOverImportStatement(): Unit = {
    s"import foo.bar\n${|}def baz = { 1\n2\n }" movedUpIs "def baz = { 1\n  2\n}\nimport foo.bar"
    s"${|}def baz = { 1\n2\n }\nimport foo.bar" movedDownIs "import foo.bar\ndef baz = { 1\n  2\n}"

    //    s"${|}import foo.bar\ndef baz = { 1\n2\n }" movedDownIs "def baz = { 1\n2\n }\nimport foo.bar"
    //    s"def baz = { 1\n2\n }\n${|}import foo.bar" movedUpIs "import foo.bar\ndef baz = { 1\n2\n }"
  }

  def testIfStatement(): Unit = {
    s"${|}if (true) {\nfoo\n}\nprintln()" movedDownIs "println()\nif (true) {\n  foo\n}"

    s"${|}foo\nif (false) {\nbar\n}".moveDownIsDisabled()
  }

  def testForStatement(): Unit = {
    s"${|}for (x <- xs) {\nfoo\n}\nprintln()" movedDownIs "println()\nfor (x <- xs) {\n  foo\n}"

    s"${|}foo\nfor (x <- xs) {\nbar\n}".moveDownIsDisabled()
  }

  def testMatchStatement(): Unit = {
    s"${|}1 match {\n  case 1 => null\n}\nprintln()" movedDownIs "println()\n1 match {\n  case 1 => null\n}"
  }

  def testTryStatement(): Unit = {
    s"${|}try {\n  foo\n} catch {\n  case _ =>\n}\nprintln()" movedDownIs "println()\ntry {\n  foo\n} catch {\n  case _ =>\n}"
  }

  def testMethodCallWithBlockExpression(): Unit = {
    //    s"${|}foo()\nbar" moveDownIsDisabled;
    //    s"${|}foo() {}\nbar" moveDownIsDisabled;

    s"${|}foo() {\nfoo\n}\nbar" movedDownIs "bar\nfoo() {\n  foo\n}"
  }
}

class Scala3StatementMoverTest extends ScalaStatementMoverTest {
  override protected def supportedIn(version: ScalaVersion): Boolean =
    version >= LatestScalaVersions.Scala_3_0

  def testBracelessOneMethodExtensionMoveUp(): Unit = {
    val fileText =
      s"""object Scope {
         |  class MyClass
         |  opaque type MyOpaqueType = String
         |
         |  extension (t: MyClass)
         |    def myExtension1: String = ""
         |    def myExtension2: String = ""
         |    def myExtension3: String = ""
         |
         |  exten${|}sion (t: MyOpaqueType)
         |    def myExtension11: String = ""
         |}""".stripMargin

    val expectedResult =
      """object Scope {
        |  class MyClass
        |  opaque type MyOpaqueType = String
        |
        |  extension (t: MyOpaqueType)
        |    def myExtension11: String = ""
        |
        |  extension (t: MyClass)
        |    def myExtension1: String = ""
        |    def myExtension2: String = ""
        |    def myExtension3: String = ""
        |}""".stripMargin

    fileText.movedUpIs(expectedResult)
  }

  def testBracelessMultipleMethodsExtensionMoveDown(): Unit = {
    val fileText =
      s"""object Scope {
         |  class MyClass
         |  opaque type MyOpaqueType = String
         |
         |  ${|}extension (t: MyClass)
         |    def myExtension1: String = ""
         |    def myExtension2: String = ""
         |    def myExtension3: String = ""
         |
         |  extension (t: MyOpaqueType)
         |    def myExtension11: String = ""
         |}""".stripMargin

    val expectedResult =
      """object Scope {
        |  class MyClass
        |  opaque type MyOpaqueType = String
        |
        |  extension (t: MyOpaqueType)
        |    def myExtension11: String = ""
        |
        |  extension (t: MyClass)
        |    def myExtension1: String = ""
        |    def myExtension2: String = ""
        |    def myExtension3: String = ""
        |}""".stripMargin

    fileText.movedDownIs(expectedResult)
  }

  def testBracelessOneMethodExtensionMoveDown(): Unit = {
    val fileText =
      s"""object Scope {
         |  class MyClass
         |  opaque type MyOpaqueType = String
         |
         |  exten${|}sion (t: MyOpaqueType)
         |    def myExtension11: String = ""
         |
         |  extension (t: MyClass)
         |    def myExtension1: String = ""
         |    def myExtension2: String = ""
         |    def myExtension3: String = ""
         |
         |}""".stripMargin

    val expectedResult =
      """object Scope {
        |  class MyClass
        |  opaque type MyOpaqueType = String
        |
        |  extension (t: MyClass)
        |    def myExtension1: String = ""
        |    def myExtension2: String = ""
        |    def myExtension3: String = ""
        |
        |  extension (t: MyOpaqueType)
        |    def myExtension11: String = ""
        |
        |}""".stripMargin

    fileText.movedDownIs(expectedResult)
  }

  def testBracelessMultipleMethodsExtensionMoveUp(): Unit = {
    val fileText =
      s"""object Scope {
         |  class MyClass
         |  opaque type MyOpaqueType = String
         |
         |  extension (t: MyOpaqueType)
         |    def myExtension11: String = ""
         |
         |  ${|}extension (t: MyClass)
         |    def myExtension1: String = ""
         |    def myExtension2: String = ""
         |    def myExtension3: String = ""
         |}""".stripMargin

    val expectedResult =
      """object Scope {
        |  class MyClass
        |  opaque type MyOpaqueType = String
        |
        |  extension (t: MyClass)
        |    def myExtension1: String = ""
        |    def myExtension2: String = ""
        |    def myExtension3: String = ""
        |
        |  extension (t: MyOpaqueType)
        |    def myExtension11: String = ""
        |}""".stripMargin

    fileText.movedUpIs(expectedResult)
  }

  def testBracelessOneMethodExtensionMoveUpOutsideBlock(): Unit = {
    val fileText =
      s"""object Scope {
         |
         |  class MyClass
         |  opaque type MyOpaqueType = String
         |  locally {
         |    ${|}extension (t: MyOpaqueType)
         |      def myExtension11: String = ""
         |
         |    extension (t: MyClass)
         |      def myExtension1: String = ""
         |      def myExtension2: String = ""
         |      def myExtension3: String = ""
         |  }
         |}""".stripMargin

    val expectedResult =
      """object Scope {
        |
        |  class MyClass
        |  opaque type MyOpaqueType = String
        |  extension (t: MyOpaqueType)
        |    def myExtension11: String = ""
        |  locally {
        |
        |    extension (t: MyClass)
        |      def myExtension1: String = ""
        |      def myExtension2: String = ""
        |      def myExtension3: String = ""
        |  }
        |}""".stripMargin

    fileText.movedUpIs(expectedResult)
  }

  def testBracelessOneMethodExtensionMoveDownInsideBlock(): Unit = {
    val fileText =
      s"""object Scope {
         |
         |  class MyClass
         |  opaque type MyOpaqueType = String
         |  ${|}extension (t: MyOpaqueType)
         |    def myExtension11: String = ""
         |  locally {
         |
         |    extension (t: MyClass)
         |      def myExtension1: String = ""
         |      def myExtension2: String = ""
         |      def myExtension3: String = ""
         |  }
         |}""".stripMargin

    val expectedResult =
      """object Scope {
        |
        |  class MyClass
        |  opaque type MyOpaqueType = String
        |  locally {
        |    extension (t: MyOpaqueType)
        |      def myExtension11: String = ""
        |
        |    extension (t: MyClass)
        |      def myExtension1: String = ""
        |      def myExtension2: String = ""
        |      def myExtension3: String = ""
        |  }
        |}""".stripMargin

    fileText.movedDownIs(expectedResult)
  }
}
