package org.jetbrains.plugins.scala.conversion.generated

import com.intellij.pom.java.LanguageLevel
import org.jetbrains.plugins.scala.conversion.JavaToScalaConversionTestBase
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

class JavaToScalaConversionExamplesTest extends JavaToScalaConversionTestBase {

  import org.jetbrains.plugins.scala.util.TypeAnnotationSettings._

  //This class was generated by build script, please don't change this
  override def folderPath: String = super.folderPath + "examples/"

  def testAnnotated(): Unit = doTest()

  def testAnonymousClass(): Unit = doTest()

  def testAnonymousClassWithStaticMethods(): Unit = doTest()

  def testDeprecated(): Unit = doTest()

  def testEnum(): Unit = doTest()

  def testFinalInObjects(): Unit = doTest()

  def testHelloWorld(): Unit = doTest()

  def testInterface(): Unit = doTest()

  def testRightOrder(): Unit = doTest()

  def testStaticInitializer(): Unit = doTest()

  def testStaticPrefix(): Unit = doTest()

  def testThrows(): Unit = doTest()

  def testTypeParameters(): Unit = doTest()

  def testVarArgs(): Unit = doTest()

  def testSCL3899(): Unit = doTest()

  def testSCL9369(): Unit = doTest()

  def testSCL11463(): Unit = doTest()

  def testNeedConstructorsSorting(): Unit = doTest()

  def testNoOverrideToImplement(): Unit = {
    val projectSettings = ScalaProjectSettings.getInstance(getProject)
    val oldValue = projectSettings.isAddOverrideToImplementInConverter

    projectSettings.setAddOverrideToImplementInConverter(false)
    doTest()
    projectSettings.setAddOverrideToImplementInConverter(oldValue)
  }

  //SCL-9434
  //NOTE: it currently converts to AnyRef instead of Any
  def testSCL9434(): Unit = doTest()

  def testSCL9421(): Unit = doTest()

  def testSCL9375(): Unit = doTest()

  def testSCL11313(): Unit = doTest()

  def testSCL11451(): Unit = doTest()

  def testNoReturnTypeForPublic(): Unit =
    doTest(noTypeAnnotationForPublic(
      alwaysAddType(getDefaultSettings))
    )

  def testNoReturnTypeForLocal(): Unit =
    doTest(noTypeAnnotationForLocal(
      alwaysAddType(getDefaultSettings)
    ))


  def testNullReturnValue_DefaultSettings(): Unit = {
    doTest(getDefaultSettings, "NullReturnValue.java")
  }

  def testNullReturnValue_RemoveAnnotationsSettings(): Unit = {
    doTest(getRemoveTypeAnnotationsForAllMembersSettings, "NullReturnValue.java")
  }

  def testAbstractMethods_DefaultSettings(): Unit = {
    doTest(getDefaultSettings, "AbstractMethods.java")
  }

  def testAbstractMethods_RemoveAnnotationsSettings(): Unit = {
    doTest(getRemoveTypeAnnotationsForAllMembersSettings, "AbstractMethods.java")
  }

  def testImports(): Unit = doTest()

  def testLambdaExpr(): Unit = doTest()

  def testLambdaExprWithReturnAtTailPosition(): Unit = doTest(getDefaultSettings)

  def testLambdaExprWithReturnInMiddlePosition(): Unit = doTest(getDefaultSettings)

  def testSwitchExpression(): Unit = doTest()

  def testSwitchRemovableBreak(): Unit = doTest()

  def testSwitchNonRemovableBreak(): Unit = doTest()

  def testBreakWithLabel(): Unit = doTest()

  def testSwitchExpressionYield(): Unit = doTestJava14()

  def testSwitchExpressionYieldNonRemovable(): Unit = doTestJava14()

  def testSwitchMultiLabel(): Unit = doTestJava14()

  def testJavaPatternMatchingExamples(): Unit = doTestJava20_preview()

  def testCountingLoopSimple(): Unit = doTest()

  def testCountingLoopInclusive(): Unit = doTest()

  def testCountingLoopDescending(): Unit = doTest()

  def testCountingLoopDescendingInclusive(): Unit = doTest()

  def testNonCountingIncrementLoop(): Unit = doTest()

  //SCL-11427
  def testDontMakeClassParameterAFieldWhenNotNeeded(): Unit = doTest()

  //partially SCL-11326
  def testPreserveFieldModifiersAndAnnotations(): Unit = doTest()

  private def doTestJava14(): Unit = {
    doTestJavaWithVersion(LanguageLevel.JDK_14)
  }

  private def doTestJava20_preview(): Unit = {
    doTestJavaWithVersion(LanguageLevel.JDK_20_PREVIEW)
  }

  private def doTestJavaWithVersion(languageLevel: LanguageLevel): Unit = {
    import com.intellij.openapi.roots.LanguageLevelProjectExtension
    val projectExtension = LanguageLevelProjectExtension.getInstance(getProject)
    val oldLevel = projectExtension.getLanguageLevel
    try {
      projectExtension.setLanguageLevel(languageLevel)
      doTest()
    } finally {
      projectExtension.setLanguageLevel(oldLevel)
    }
  }
}