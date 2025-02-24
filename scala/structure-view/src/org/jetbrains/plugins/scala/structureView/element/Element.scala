package org.jetbrains.plugins.scala.structureView.element

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.navigation.ColoredItemPresentation
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScBlockExpr
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScExtension, ScFunction, ScTypeAlias, ScValue, ScVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

trait Element extends StructureViewTreeElement with ColoredItemPresentation {
  def element: PsiElement

  def inherited: Boolean

  def isAlwaysLeaf: Boolean

  def isAlwaysShowsPlus: Boolean
}

object Element {

  def forPsi(element: PsiElement, inherited: Boolean = false): Seq[Element] = element match {
    // TODO Type definition can be inherited
    case definition: ScTypeDefinition => Seq(new TypeDefinition(definition))
    case packaging: ScPackaging       => packaging.getChildren.flatMap(Element.forPsi(_)).toSeq
    case parameter: ScClassParameter  => Seq(new ValOrVarParameter(parameter, inherited))
    case function: ScFunction         => Seq(new Function(function, inherited))
    case variable: ScValue            => variable.declaredElements.map(new Value(_, inherited))
    case variable: ScVariable         => variable.declaredElements.map(new Variable(_, inherited))
    case alias: ScTypeAlias           => Seq(new TypeAlias(alias, inherited))
    case block: ScBlockExpr           => Seq(new Block(block))
    case extension: ScExtension       => Seq(new Extension(extension))
    case _                            => Seq.empty
  }

  def apply(fileProvider: () => ScalaFile): Element = new File(fileProvider)
}