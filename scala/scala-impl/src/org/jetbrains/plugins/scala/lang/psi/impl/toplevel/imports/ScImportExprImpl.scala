package org.jetbrains.plugins.scala.lang.psi.impl.toplevel
package imports

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.{ObjectExt, StubBasedExt}
import org.jetbrains.plugins.scala.lang.TokenSets.IMPORT_WILDCARDS
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType._
import org.jetbrains.plugins.scala.lang.psi.ScImportsOrExportsHolder
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports._
import org.jetbrains.plugins.scala.lang.psi.impl.{ScalaPsiElementFactory, ScalaStubBasedElementImpl}
import org.jetbrains.plugins.scala.lang.psi.stubs.ScImportExprStub
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil.escapeKeywordsFqn

class ScImportExprImpl private (stub: ScImportExprStub, node: ASTNode)
  extends ScalaStubBasedElementImpl(stub, IMPORT_EXPR, node) with ScImportExpr {

  def this(node: ASTNode) = this(null, node)

  def this(stub: ScImportExprStub) = this(stub, null)

  override def toString: String = "ImportExpression"

  override def hasWildcardSelector: Boolean = byStubOrPsi(_.hasWildcardSelector)(wildcardElement.nonEmpty)

  override def hasGivenSelector: Boolean = byStubOrPsi(_.hasGivenSelector)(selectors.exists(_.isGivenSelector))

  override def wildcardElement: Option[PsiElement] =
    Option(findChildByType(IMPORT_WILDCARDS))
      .orElse(selectorSet.flatMap(_.wildcardElement))

  override def qualifier: Option[ScStableCodeReference] =
    reference.flatMap(ref =>
      if (hasWildcardSelector || selectorSet.isDefined)
        Some(ref)
      else
        ref.qualifier
    )

  override def deleteExpr(): Unit = {
    val parent = getParent.asInstanceOf[ScImportOrExportStmt]
    if (parent.importExprs.size == 1) {
      parent.getParent match {
        case x: ScImportsOrExportsHolder => x.deleteImportOrExportStmt(parent)
        case _ =>
      }
    } else {
      val node = parent.getNode
      val remove = node.removeChild _
      val next = getNextSibling
      if (next != null) {
        def removeWhitespaceAfterComma(comma: ASTNode): Unit = {
          if (comma.getTreeNext != null && !comma.getTreeNext.getText.contains("\n") &&
            comma.getTreeNext.getText.trim.isEmpty) {
            remove(comma.getTreeNext)
          }
        }
        if (next.textMatches(",")) {
          val comma = next.getNode
          removeWhitespaceAfterComma(comma)
          remove(comma)
        } else {
          if (next.getNextSibling != null && next.getNextSibling.textMatches(",")) {
            val comma = next.getNextSibling
            removeWhitespaceAfterComma(comma.getNode)
            remove(next.getNode)
            remove(comma.getNode)
          } else {
            val prev = getPrevSibling
            if (prev != null) {
              if (prev.textMatches(",")) {
                remove(prev.getNode)
              } else {
                if (prev.getPrevSibling != null && prev.getPrevSibling.textMatches(",")) {
                  remove(prev.getPrevSibling.getNode)
                }
              }
            }
          }
        }
      } else {
        val prev = getPrevSibling
        if (prev != null) {
          if (prev.textMatches(",")) {
            remove(prev.getNode)
          } else {
            if (prev.getPrevSibling != null && prev.getPrevSibling.textMatches(",")) {
              val prevSibling = prev.getPrevSibling
              remove(prev.getNode)
              remove(prevSibling.getNode)
            }
          }
        }
      }
      remove(getNode)
    }
  }

  override def deleteRedundantSingleSelectorBraces(): Unit = {
    this.selectors match {
      case Seq(selector: ScImportSelector) =>
        if (!selector.isScala2StyleAliasImport) {
          val isGivenSelector = selector.isGivenSelector
          val prefix = this.qualifier match {
            case Some(qual) => qual.getText.pipeIf(isGivenSelector)(escapeKeywordsFqn) + "."
            case None       => ""
          }
          val textWithoutBraces = prefix + selector.getText
          this.replace(ScalaPsiElementFactory.createImportExprFromText(textWithoutBraces, this, escapeKeywords = !isGivenSelector))
        }
      case _ =>
    }
  }

  override def selectorSet: Option[ScImportSelectors] =
    this.stubOrPsiChild(IMPORT_SELECTORS)

  override def reference: Option[ScStableCodeReference] =
    byPsiOrStub(getFirstChild.asOptionOf[ScStableCodeReference])(_.reference)
}
