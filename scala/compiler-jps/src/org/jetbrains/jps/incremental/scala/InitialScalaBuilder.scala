package org.jetbrains.jps.incremental.scala

import com.intellij.openapi.util.Key
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.java.{JavaBuilderUtil, JavaSourceRootDescriptor}
import org.jetbrains.jps.incremental._
import org.jetbrains.jps.incremental.messages.{BuildMessage, CompilerMessage}
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType

import _root_.java.io._
import _root_.java.nio.charset.StandardCharsets
import _root_.java.nio.file.Files
import _root_.java.{util => ju}
import _root_.scala.jdk.CollectionConverters._
import _root_.scala.util.Try


/**
  * For tasks that should be performed once per compilation
  */
class InitialScalaBuilder extends ModuleLevelBuilder(BuilderCategory.SOURCE_INSTRUMENTER) { //should be before other scala builders

  import InitialScalaBuilder._

  override def getPresentableName: String = JpsBundle.message("scala.compiler.metadata.builder")

  override def buildStarted(context: CompileContext): Unit = {
    collectAndStoreScalaModules(context)
  }

  override def buildFinished(context: CompileContext): Unit = {
    val previousIncrementalityType = readPreviousIncrementalityType(context)
    val incrementalityType = ScalaBuilder.projectSettings(context).getIncrementalityType

    previousIncrementalityType match {
      case Some(`incrementalityType`) =>
      case _ =>
        incrementalityTypeStorageFile(context).foreach { file =>
          val parentDir = file.getParentFile
          if (!parentDir.exists()) parentDir.mkdirs()
          Try(Files.write(file.toPath, incrementalityType.name().getBytes(StandardCharsets.UTF_8)))
        }
    }
  }

  override def build(context: CompileContext,
                     chunk: ModuleChunk,
                     dirtyFilesHolder: DirtyFilesHolder[JavaSourceRootDescriptor, ModuleBuildTarget],
                     outputConsumer: ModuleLevelBuilder.OutputConsumer): ModuleLevelBuilder.ExitCode = {

    val previousIncrementalityType = readPreviousIncrementalityType(context)
    val incrementalityType = ScalaBuilder.projectSettings(context).getIncrementalityType

    previousIncrementalityType match {
      case _ if JavaBuilderUtil.isForcedRecompilationAllJavaModules(context) =>
        ModuleLevelBuilder.ExitCode.NOTHING_DONE
      case Some(`incrementalityType`) =>
        ModuleLevelBuilder.ExitCode.NOTHING_DONE
      case _ =>
        val message = JpsBundle.message("incremental.compiler.changed.rebuild")
        context.processMessage(new CompilerMessage("scala", BuildMessage.Kind.JPS_INFO, message))
        ModuleLevelBuilder.ExitCode.CHUNK_REBUILD_REQUIRED
    }
  }

  override def getCompilableFileExtensions: ju.List[String] = ju.Arrays.asList("scala", "java")

  private def incrementalityTypeStorageFile(context: CompileContext): Option[File] =
    Option(context.getProjectDescriptor.dataManager.getDataPaths.getDataStorageRoot)
      .map(new File(_, "incrementalType.dat"))

  private def readPreviousIncrementalityType(context: CompileContext): Option[IncrementalityType] =
    incrementalityTypeStorageFile(context).filter(_.exists()).flatMap { file =>
      val result = Try {
        val str = new String(Files.readAllBytes(file.toPath), StandardCharsets.UTF_8)
        IncrementalityType.valueOf(str)
      }.toOption
      if (result.isEmpty) file.delete()
      result
    }
}

object InitialScalaBuilder {

  private val scalaModulesKey: Key[Set[JpsModule]] =
    Key.create[Set[JpsModule]]("jps.scala.modules")

  def hasScala(context: CompileContext, module: JpsModule): Boolean =
    Option(context.getUserData(scalaModulesKey)).exists(_.contains(module))

  def hasScalaModules(context: CompileContext, chunk: ModuleChunk): Boolean =
    chunk.getModules.asScala.exists(hasScala(context, _))

  def isScalaProject(context: CompileContext): Boolean =
    Option(context.getUserData(scalaModulesKey)).exists(_.nonEmpty)


  private def storeScalaModules(context: CompileContext, scalaModules: Set[JpsModule]): Unit = {
    context.putUserData(scalaModulesKey, scalaModules)
  }

  private def collectAndStoreScalaModules(context: CompileContext): Unit = {
    val result = context.getProjectDescriptor.getProject.getModules.asScala
      .filter(SettingsManager.getScalaSdk(_).isDefined)
      .toSet

    storeScalaModules(context, result)
  }
}
