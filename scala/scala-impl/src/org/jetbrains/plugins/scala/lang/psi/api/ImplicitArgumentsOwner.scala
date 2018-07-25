package org.jetbrains.plugins.scala
package lang.psi.api

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, TraversableExt}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScArgumentExprList, ScExpression}
import org.jetbrains.plugins.scala.lang.psi.types.nonvalue.Parameter
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult

/**
 * Nikolay.Tropin
 * 2014-10-17
 */
// TODO Implement selectively, not by ScExpression
trait ImplicitArgumentsOwner extends ScalaPsiElement {
  @volatile
  private var implicitArguments: Option[Seq[ScalaResolveResult]] = None

  private[psi] final def setImplicitArguments(results: Option[Seq[ScalaResolveResult]]): Unit = {
    implicitArguments = results
  }

  //todo: get rid of side-effect-driven logic
  def findImplicitArguments: Option[Seq[ScalaResolveResult]] = {
    ProgressManager.checkCanceled()

    updateImplicitArguments()

    implicitArguments
  }

  //calculation which may set implicit arguments as a side effect, typically computation of a type
  protected def updateImplicitArguments(): Unit

  def matchedParameters: Seq[(ScExpression, Parameter)] = Seq.empty

  def explicitImplicitArgList: Option[ScArgumentExprList] = {
    val implicitArg = matchedParameters.collectFirst {
      case (arg, param) if param.isImplicit => arg
    }
    implicitArg.toSeq
      .flatMap(_.parentsInFile.take(2)) //argument or rhs of a named argument
      .filterBy[ScArgumentExprList]
      .headOption
  }
}

object ImplicitArgumentsOwner {
  def unapply(e: ImplicitArgumentsOwner): Option[Seq[ScalaResolveResult]] = e.findImplicitArguments
}
