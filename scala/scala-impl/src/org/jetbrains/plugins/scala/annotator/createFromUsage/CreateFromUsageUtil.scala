package org.jetbrains.plugins.scala.annotator.createFromUsage

import com.intellij.codeInsight.template.TemplateBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiClass, PsiElement}
import org.jetbrains.plugins.scala.codeInspection.collections.MethodRepr
import org.jetbrains.plugins.scala.extensions.{IteratorExt, PsiElementExt, ResolvesTo}
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns._
import org.jetbrains.plugins.scala.lang.psi.api.base.types.{ScParameterizedTypeElement, ScSimpleTypeElement, ScTupleTypeElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScConstructorInvocation, ScLiteral, ScReference}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScAssignment, ScExpression, ScParenthesisedExpr, ScReferenceExpression}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.{ScParameter, ScTypeParam}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.types.api.{Any, ExtractClass}
import org.jetbrains.plugins.scala.lang.psi.types.result._
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt}
import org.jetbrains.plugins.scala.lang.refactoring.namesSuggester.NameSuggester
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.plugins.scala.util.TypeAnnotationUtil

object CreateFromUsageUtil {

  def uniqueNames(names: Seq[String]): List[String] = {
    names.foldLeft(List[String]()) { (r, h) =>
      (h #:: LazyList.from(1).map(h + _)).find(!r.contains(_)).get :: r
    }.reverse
  }

  def nameByType(tp: ScType): String = NameSuggester.suggestNamesByType(tp).headOption.getOrElse("value")

  private def nameAndTypeForArg(arg: PsiElement): (String, ScType) = {
    implicit val project: ProjectContext = arg.projectContext

    arg match {
      case ScParenthesisedExpr(inner) =>
        nameAndTypeForArg(inner)
      case lit: ScLiteral =>
        val tp = lit.`type`().getOrAny.widenIfLiteral
        (nameByType(tp), tp)
      case ref: ScReferenceExpression =>
        (ref.refName, ref.`type`().getOrAny)
      case ScAssignment(ref: ScReferenceExpression, value) =>
        val name = ref.getText
        val tp   = value.map(nameAndTypeForArg).map(_._2).asTypeResult.getOrAny
        (name, tp)
      case expr: ScExpression =>
        val tp = expr.`type`().getOrAny
        (nameByType(tp), tp)
      case bp: ScBindingPattern if !bp.isWildcard =>
        (bp.name, bp.`type`().getOrAny)
      case p: ScPattern =>
        val tp: ScType = p.`type`().getOrAny
        (nameByType(tp), tp)
      case _ =>
        ("value", Any)
    }
  }

  def paramsText(args: Seq[PsiElement]): String = {
    val (names, types) = args.map(nameAndTypeForArg).unzip
    (uniqueNames(names) lazyZip types).map((name, tpe) => s"$name: ${tpe.canonicalText}").mkString("(", ", ", ")")
  }

  def parametersText(ref: ScReference): String = {
    ref.getParent match {
      case p: ScPattern =>
        paramsText(patternArgs(p))
      case MethodRepr(_, _, _, args) => paramsText(args) //for case class
      case _ =>
        val fromConstrArguments = PsiTreeUtil.getParentOfType(ref, classOf[ScConstructorInvocation]) match {
          case ScConstructorInvocation(simple: ScSimpleTypeElement, args) if ref.getParent == simple => args
          case ScConstructorInvocation(pt: ScParameterizedTypeElement, args) if ref.getParent == pt.typeElement => args
          case _ => Seq.empty
        }
        fromConstrArguments.map(argList => paramsText(argList.exprs)).mkString
    }
  }

  def patternArgs(pattern: ScPattern): Seq[ScPattern] = {
    pattern match {
      case cp: ScConstructorPattern => cp.args.patterns
      case inf: ScInfixPattern => inf.left +: inf.rightOption.toSeq
      case _ => Seq.empty
    }
  }

  def addParametersToTemplate(elem: PsiElement, builder: TemplateBuilder): Unit = {
    elem.depthFirst().filterByType[ScParameter].foreach { parameter =>
      val id = parameter.getNameIdentifier
      builder.replaceElement(id, id.getText)

      parameter.paramType.foreach { it =>
        builder.replaceElement(it, it.getText)
      }
    }
  }

  def addTypeParametersToTemplate(elem: PsiElement, builder: TemplateBuilder): Unit = {
    elem.depthFirst().filterByType[ScTypeParam].foreach { tp =>
      builder.replaceElement(tp.nameId, tp.name)
    }
  }

  def addQmarksToTemplate(elem: PsiElement, builder: TemplateBuilder): Unit = {
    val Q_MARKS = "???"
    elem.depthFirst().filterByType[ScReferenceExpression].filter(_.textMatches(Q_MARKS))
            .foreach { qmarks =>
      builder.replaceElement(qmarks, Q_MARKS)
    }
  }

  def addUnapplyResultTypesToTemplate(fun: ScFunction, builder: TemplateBuilder): Unit = {
    TypeAnnotationUtil.removeTypeAnnotationIfNeeded(fun)

    fun.returnTypeElement match {
      case Some(ScParameterizedTypeElement(_, Seq(tuple: ScTupleTypeElement))) => //Option[(A, B)]
        tuple.components.foreach(te => builder.replaceElement(te, te.getText))
      case Some(ScParameterizedTypeElement(_, args)) =>
        args.foreach(te => builder.replaceElement(te, te.getText))
      case _ =>
    }
  }

  def positionCursor(element: PsiElement): Editor = {
    val offset = element.getTextRange.getEndOffset
    val project = element.getProject
    val descriptor = new OpenFileDescriptor(project, element.getContainingFile.getVirtualFile, offset)
    FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
  }

  def unapplyMethodText(pattern: ScPattern): String = {
    import pattern.projectContext
    val pType = pattern.expectedType.getOrElse(Any)
    val pName = nameByType(pType)
    s"def unapply($pName: ${pType.canonicalText}): ${unapplyMethodTypeText(pattern)} = ???"
  }

  def unapplyMethodTypeText(pattern: ScPattern): String = {
    val types = CreateFromUsageUtil.patternArgs(pattern).map(_.`type`().getOrAny)
    val typesText = types.map(_.canonicalText).mkString(", ")
    types.size match {
      case 0 => "Boolean"
      case 1 => s"Option[$typesText]"
      case _ => s"Option[($typesText)]"
    }
  }
}

object InstanceOfClass {
  def unapply(elem: PsiElement): Option[PsiClass] = {
    elem match {
      case ScExpression.Type(TypeAsClass(psiClass)) => Some(psiClass)
      case ResolvesTo(typed: ScTypedDefinition) =>
        typed.`type`().toOption match {
          case Some(TypeAsClass(psiClass)) => Some(psiClass)
          case _ => None
        }
      case _ => None
    }
  }
}

object TypeAsClass {
  def unapply(scType: ScType): Option[PsiClass] = scType match {
    case ExtractClass(aClass) => Some(aClass)
    case t: ScType => t.extractDesignatorSingleton.flatMap(_.extractClass)
    case _ => None
  }
}
