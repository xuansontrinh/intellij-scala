package org.jetbrains.plugins.scala.decompiler

import junit.framework.TestCase
import org.junit.Assert._

import java.nio.file.{Files, Path}
import scala.util.control.NonFatal

// TODO Prettify and unify Scala 2 decompiler output, SCL-20672

// self type class with

// fix: final val v: 123 = ??? is not a constant
// fix: private val parameters in value classes
// fix: implicit implicit val x: Int, implicit val y: Int
// fix: annotation on types
// fix: qualified private abstract type is decompiled, without access modifier
// fix: private type alias is decompiled
// fix: custom self type names

// Foo.this.A for type parameters
// qualified private that is equivalent to private is decompiled?
// literal types - annotations?
// why private[Class1] def method1: Int is decompiled?
// why private object PrivateObject is decompiled?

class DecompilerTest2 extends TestCase {

  def testAnnotationMembers(): Unit = doTest("annotation/Members")
  def testAnnotationMultiple(): Unit = doTest("annotation/Multiple")
  def testAnnotationParameters(): Unit = doTest("annotation/Parameters")
  def testAnnotationText(): Unit = doTest("annotation/Text")
  def testMemberBeanProperty(): Unit = doTest("member/BeanProperty")
  def testMemberBounds(): Unit = doTest("member/Bounds")
  def testMemberDef(): Unit = doTest("member/Def")
  def testMemberIdentifiers(): Unit = doTest("member/Identifiers")
  def testMemberModifiers(): Unit = doTest("member/Modifiers")
  def testMemberQualifier(): Unit = doTest("member/Qualifier")
  def testMemberThis(): Unit = doTest("member/This")
  def testMemberType(): Unit = doTest("member/Type")
  def testMemberVal(): Unit = doTest("member/Val")
  def testMemberVar(): Unit = doTest("member/Var")
  def testPackage1Package(): Unit = doTest("package1/package")
  def testPackage1Package2Package(): Unit = doTest("package1/package2/package")
  def testPackage1Package2Nested(): Unit = doTest("package1/package2/Nested")
  def testPackage1Package2NestedImport(): Unit = doTest("package1/package2/NestedImport")
  def testPackage1Package2Prefix(): Unit = doTest("package1/package2/Prefix")
  def testPackage1Package2Scope(): Unit = doTest("package1/package2/Scope")
  def testPackage1Members(): Unit = doTest("package1/Members")
  def testPackage1Paths(): Unit = doTest("package1/Root", simpleTypes = false)
  def testParameterBounds(): Unit = doTest("parameter/Bounds")
  def testParameterByName(): Unit = doTest("parameter/ByName")
  def testParameterCaseClass(): Unit = doTest("parameter/CaseClass")
  def testParameterClass(): Unit = doTest("parameter/Class")
  def testParameterContextBounds(): Unit = doTest("parameter/ContextBounds")
  def testParameterDef(): Unit = doTest("parameter/Def")
  def testParameterDefaultArguments(): Unit = doTest("parameter/DefaultArguments")
  def testParameterHKT(): Unit = doTest("parameter/HKT")
  def testParameterHKTBounds(): Unit = doTest("parameter/HKTBounds")
  def testParameterHKTVariance(): Unit = doTest("parameter/HKTVariance")
  def testParameterIdentifiers(): Unit = doTest("parameter/Identifiers")
  def testParameterModifiers(): Unit = doTest("parameter/Modifiers")
  def testParameterQualifier(): Unit = doTest("parameter/Qualifier")
  def testParameterRepeated(): Unit = doTest("parameter/Repeated")
  def testParameterTrait(): Unit = doTest("parameter/Trait")
  def testParameterType(): Unit = doTest("parameter/Type")
  def testParameterVariance(): Unit = doTest("parameter/Variance") // TODO TypeMember
  def testTypeDefinitionClass(): Unit = doTest("typeDefinition/Class")
  def testTypeDefinitionCompanions(): Unit = doTest("typeDefinition/Companions")
  def testTypeDefinitionIdentifiers(): Unit = doTest("typeDefinition/Identifiers")
  def testTypeDefinitionImplicitClass(): Unit = doTest("typeDefinition/ImplicitClass")
  def testTypeDefinitionMembers(): Unit = doTest("typeDefinition/Members")
  def testTypeDefinitionModifiers(): Unit = doTest("typeDefinition/Modifiers")
  def testTypeDefinitionObject(): Unit = doTest("typeDefinition/Object")
  def testTypeDefinitionPrivate(): Unit = doTest("typeDefinition/PackagePrivate")
  def testTypeDefinitionParents(): Unit = doTest("typeDefinition/Parents")
  def testTypeDefinitionQualifier(): Unit = doTest("typeDefinition/Qualifier")
  def testTypeDefinitionSelfType(): Unit = doTest("typeDefinition/SelfType")
  def testTypeDefinitionTrait(): Unit = doTest("typeDefinition/Trait")
//  def testTypesAnnotated(): Unit = doTest("types/Annotated") // SCL-21207
  def testTypesCompound(): Unit = doTest("types/Compound")
  def testTypesConstant(): Unit = doTest("types/Constant")
  def testTypesFunction(): Unit = doTest("types/Function")
  def testTypesIdent(): Unit = doTest("types/Ident")
//  def testTypesInfix(): Unit = doTest("types/Infix")
  def testTypesLiteral(): Unit = doTest("types/Literal")
  def testTypesParameterized(): Unit = doTest("types/Parameterized")
  def testTypesProjection(): Unit = doTest("types/Projection")
  def testTypesRefinement(): Unit = doTest("types/Refinement")
  def testTypesRefs(): Unit = doTest("types/Refs")
  def testTypesSelect(): Unit = doTest("types/Select")
  def testTypesSingleton(): Unit = doTest("types/Singleton")
  def testTypesThis(): Unit = doTest("types/This")
  def testTypesTuple(): Unit = doTest("types/Tuple")
  def testTypesWildcard(): Unit = doTest("types/Wildcard")
  def testEmptyPackage(): Unit = doTest("EmptyPackage")
  def testNesting(): Unit = doTest("Nesting")

  private def doTest(path: String, simpleTypes: Boolean = true): Unit = {
    val testDataPath = {
      val path = Path.of("scala/decompiler/testdata")
      if (Files.exists(path)) path else Path.of("community", path.toString)
    }
    assert(Files.exists(testDataPath), testDataPath.toAbsolutePath)

    val scalaFile = testDataPath.resolve(path + ".scala")
    assertTrue(s"File $scalaFile does not exist", Files.exists(scalaFile))

    val sigFile = Path.of(scalaFile.toString.replaceFirst("\\.scala$", ".sig"))
    assertTrue(s"File $sigFile doest not exist", Files.exists(sigFile))

    val Some((sourceFile, actual)) = try {
      Decompiler.sourceNameAndText(sigFile.toFile.getName, readBytes(sigFile))
    } catch {
      case NonFatal(e) =>
        Console.err.println(scalaFile)
        throw e
    }

    assertEquals("Scala file name", scalaFile.toFile.getName, sourceFile)

    val expected = new String(readBytes(scalaFile))
      .replaceAll(raw"(?s)/\*\*/.*?/\*(.*?)\*/", "$1")
      .replace("\r", "")

    val adjusted = if (!simpleTypes) actual else actual
      .replace("_root_.", "")
      .replace("java.lang.", "")
      .replace("scala.Predef.", "")
      .replaceAll("scala\\.(?!\\w+\\.(?!type))", "")
      .replaceAll("\\w+\\.this\\.(?!type)", "")
      .replaceAll("\\w+\\.(?=this.type)", "")

    assertEquals(s"Content for $path", expected, adjusted)
  }

  private def readBytes(file: Path): Array[Byte] = Files.readAllBytes(file)
}
