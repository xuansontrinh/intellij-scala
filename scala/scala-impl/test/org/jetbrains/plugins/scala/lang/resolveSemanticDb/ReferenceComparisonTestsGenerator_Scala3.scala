package org.jetbrains.plugins.scala.lang.resolveSemanticDb

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ThrowableRunnable
import org.jetbrains.plugins.scala.extensions.StringExt
import org.jetbrains.plugins.scala.lang.resolveSemanticDb.ReferenceComparisonTestBase.Result
import org.jetbrains.plugins.scala.util.TestUtils

import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable
import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
 * Generates [[org.jetbrains.plugins.scala.lang.resolveSemanticDb.generated.ReferenceComparisonTest_Scala3]]
 */
object ReferenceComparisonTestsGenerator_Scala3 {
  val excluded: Set[String] = Set(
    "large", "large2" // they're just very large with ~10k references/definitions
  )

  val testOutputPath: Path =
    Paths.get(TestUtils.findCommunityRoot())
      .resolve("scala/scala-impl/test/org/jetbrains/plugins/scala/lang/resolveSemanticDb/generated/ReferenceComparisonTest_Scala3.scala")

  def main(args: Array[String]): Unit = {
    run()
    System.exit(0)
  }

  def run(): Unit = {

    val builder = new StringBuilder

    builder ++=
      """
        |package org.jetbrains.plugins.scala.lang.resolveSemanticDb
        |package generated
        |
        |/**
        | * Tests reference resolving for test cases imported from dotty repository
        | *
        | * Important: This file is generated by [[org.jetbrains.plugins.scala.lang.resolveSemanticDb.ReferenceComparisonTestsGenerator_Scala3]]
        | */
        |//noinspection NameBooleanParameters
        |class ReferenceComparisonTest_Scala3 extends ReferenceComparisonTestBase_Scala3 {
        |""".stripMargin

    var testedCases = 0
    var successes = 0
    var result = Result.empty

    def testNameFromFilePath(path: Path): String =
      path.getFileName.toString.stripSuffix(".semdb")
    val testOutPaths = Files.list(ComparisonTestBase.outPath).iterator().asScala.toSeq
      .sortBy(testNameFromFilePath)((x, y) => StringUtil.naturalCompare(x, y))

    val originalTestNames = testOutPaths.map(testNameFromFilePath).toSet
    val usedTestNames = mutable.Set.empty[String]

    for {
      testOutPath <- testOutPaths
      testName = testNameFromFilePath(testOutPath)
      if !excluded(testName)
    } {

      val test: ReferenceComparisonTestBase = new ReferenceComparisonTestBase_Scala3 {
        override  def runTestRunnable(testRunnable: ThrowableRunnable[Throwable]): Unit = {
          val res = runTestToResult(testName)
          testedCases += 1
          val success = res.problems.isEmpty
          if (success)
            successes += 1
          result += res

          val normalizedTestName = testName.replace('-', '_')
          val finalTestName =
            if (usedTestNames.contains(normalizedTestName) || originalTestNames.contains(normalizedTestName))
              testName
            else normalizedTestName
          usedTestNames += finalTestName
          val testId = s"test_$finalTestName".escapeNonIdentifiers
          builder ++= raw"""  def $testId(): Unit = doTest("$testName", $success)"""
          builder += '\n'

          val progress = testedCases.toDouble / testOutPaths.size.toDouble * 100
          val successRate = (successes.toDouble / testedCases.toDouble) * 100
          println(
            s"(${progress.toInt}%) " +
              s"$testName: $success (${successRate.toInt}% $successes/$testedCases) " +
              s"| problems: ${result.problems.size} " +
              s"| refs: ${result.refCount} " +
              s"| failed to resolve: ${result.failedToResolve} (${(result.failedToResolve.toDouble / result.refCount * 100.0).toInt}%) " +
              s"| not tested: ${result.refCount - result.failedToResolve - result.testedRefs}) " +
              s"| complete correct: ${result.completeCorrect} " +
              s"| partial correct: ${result.partialCorrect} " +
              s"| incorrect resolve: ${result.incorrectResolves}"
          )
        }
      }

      test.run()
    }

    if (testedCases < 100) {
      System.err.println("Did not execute any generator tests... try setting module to scalaCommunity/scalaUltimate and copy vm options from one of the run configurations")
      System.exit(1)
    }

    builder ++= "}\n"

    println("Print...")
    Files.writeString(testOutputPath, builder)
    println("Done.")
  }
}