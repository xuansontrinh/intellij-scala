package org.jetbrains.plugins.scala.compiler.highlighting

import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.jps.incremental.scala.remote.SourceScope
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode

import scala.concurrent.duration._

private sealed trait CompilationRequest {
  val priority: Int
  val virtualFile: VirtualFile
  val document: Document
  val debugReason: String

  val compilationDelay: FiniteDuration

  private val timestamp: Long = System.nanoTime()

  def remaining: FiniteDuration = Deadline(timestamp.nanoseconds + compilationDelay).timeLeft
}

private object CompilationRequest {
  /**
   * @param isFirstTimeHighlighting whether worksheet editor has just been selected (is true every time when tabs are switched)
   */
  final case class WorksheetRequest(
    file: ScalaFile,
    virtualFile: VirtualFile,
    document: Document,
    isFirstTimeHighlighting: Boolean,
    debugReason: String,
  ) extends CompilationRequest {
    override val priority: Int = 0

    override val compilationDelay: FiniteDuration =
      if (isFirstTimeHighlighting) Duration.Zero else ScalaHighlightingMode.compilationDelay
  }

  final case class IncrementalRequest(
    module: Module,
    sourceScope: SourceScope,
    virtualFile: VirtualFile,
    document: Document,
    psiFile: PsiFile,
    debugReason: String
  ) extends CompilationRequest {
    override val priority: Int = 1

    override val compilationDelay: FiniteDuration = Duration.Zero
  }

  final case class DocumentRequest(
    module: Module,
    sourceScope: SourceScope,
    virtualFile: VirtualFile,
    document: Document,
    debugReason: String
  ) extends CompilationRequest {
    override val priority: Int = 2

    override val compilationDelay: FiniteDuration = ScalaHighlightingMode.compilationDelay
  }

  implicit val compilationRequestOrdering: Ordering[CompilationRequest] = { (x, y) =>
    if (x.priority != y.priority)
      Ordering.Int.compare(x.priority, y.priority)
    else {
      val res = Ordering.String.compare(x.virtualFile.getCanonicalPath, y.virtualFile.getCanonicalPath)
      if (res != 0) res
      else Ordering.Long.compare(x.timestamp, y.timestamp)
    }
  }
}
