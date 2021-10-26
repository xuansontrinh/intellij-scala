package org.jetbrains.plugins.scala.lang.dfa.analysis.invocations.specialSupport

import com.intellij.codeInspection.dataFlow.java.inst.{BooleanBinaryInstruction, NotInstruction, NumericBinaryInstruction}
import com.intellij.codeInspection.dataFlow.lang.ir.ControlFlow.DeferredOffset
import com.intellij.codeInspection.dataFlow.lang.ir._
import com.intellij.codeInspection.dataFlow.rangeSet.LongRangeSet
import com.intellij.codeInspection.dataFlow.types.{DfIntegralType, DfTypes}
import com.intellij.codeInspection.dataFlow.value.RelationType
import org.jetbrains.plugins.scala.extensions.PsiClassExt
import org.jetbrains.plugins.scala.lang.dfa.analysis.framework.ScalaStatementAnchor
import org.jetbrains.plugins.scala.lang.dfa.controlFlow.ScalaDfaControlFlowBuilder
import org.jetbrains.plugins.scala.lang.dfa.invocationInfo.InvocationInfo
import org.jetbrains.plugins.scala.lang.dfa.invocationInfo.arguments.Argument
import org.jetbrains.plugins.scala.lang.dfa.utils.ScalaDfaTypeConstants.LogicalOperation
import org.jetbrains.plugins.scala.lang.dfa.utils.ScalaDfaTypeConstants.Packages.{ScalaBoolean, ScalaInt, ScalaLong}
import org.jetbrains.plugins.scala.lang.dfa.utils.ScalaDfaTypeConstants.SyntheticOperators._
import org.jetbrains.plugins.scala.lang.dfa.utils.ScalaDfaTypeUtils.{balanceType, extractExpressionFromArgument, resolveExpressionType, scTypeToDfType}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.synthetic.ScSyntheticFunction

object SyntheticMethodsSpecialSupport {

  def tryTransformSyntheticFunctionSpecially(function: ScSyntheticFunction,
                                             invocationInfo: InvocationInfo,
                                             invocation: ScExpression,
                                             builder: ScalaDfaControlFlowBuilder): Boolean = {
    if (verifyArgumentsForBinaryOperator(invocationInfo.argListsInEvaluationOrder)) {
      if (tryTransformBinaryNumericOperator(function, invocationInfo, invocation, builder)) true
      else if (tryTransformBinaryRelationalOperator(function, invocationInfo, invocation, builder)) true
      else if (tryTransformBinaryLogicalOperator(function, invocationInfo, invocation, builder)) true
      else false
    } else if (verifyArgumentsForUnaryOperator(invocationInfo.argListsInEvaluationOrder)) {
      if (tryTransformUnaryNumericOperator(function, invocationInfo, invocation, builder)) true
      else if (tryTransformUnaryLogicalOperator(function, invocationInfo, invocation, builder)) true
      else false
    } else false
  }

  private def matchesSignature(function: ScSyntheticFunction, functionName: String, returnedClass: String): Boolean = {
    val properReturnedClass = function.retType.extractClass match {
      case Some(returnClass) if returnClass.qualifiedName == returnedClass => true
      case _ => false
    }
    function.name == functionName && properReturnedClass
  }

  private def verifyArgumentsForUnaryOperator(arguments: List[List[Argument]]): Boolean = {
    arguments.size == 1 && arguments.head.size == 1
  }

  private def verifyArgumentsForBinaryOperator(arguments: List[List[Argument]]): Boolean = {
    arguments.size == 1 && arguments.head.size == 2
  }

  private def argumentsForBinarySyntheticOperator(invocationInfo: InvocationInfo): (Argument, Argument) = {
    val args = invocationInfo.argListsInEvaluationOrder
    assert(verifyArgumentsForBinaryOperator(args))
    val List(leftArg, rightArg) = args.head
    (leftArg, rightArg)
  }

  private def transformBinaryOperands(leftArg: Argument, rightArg: Argument,
                                      forceEqualityByContent: Boolean,
                                      builder: ScalaDfaControlFlowBuilder): Unit = {
    val leftExpression = extractExpressionFromArgument(leftArg)
    val rightExpression = extractExpressionFromArgument(rightArg)
    val balancedType = balanceType(leftExpression.map(resolveExpressionType),
      rightExpression.map(resolveExpressionType), forceEqualityByContent)

    leftArg.content.transform(builder)
    builder.addImplicitConversion(leftExpression, balancedType)
    rightArg.content.transform(builder)
    builder.addImplicitConversion(rightExpression, balancedType)
  }

  private def tryTransformBinaryNumericOperator(function: ScSyntheticFunction, invocationInfo: InvocationInfo,
                                                invocation: ScExpression,
                                                builder: ScalaDfaControlFlowBuilder): Boolean = {
    for (typeClass <- List(ScalaInt, ScalaLong); operationName <- NumericBinary.keys) {
      if (matchesSignature(function, operationName, typeClass)) {
        val (leftArg, rightArg) = argumentsForBinarySyntheticOperator(invocationInfo)
        transformBinaryOperands(leftArg, rightArg, forceEqualityByContent = false, builder)

        builder.addInstruction(new NumericBinaryInstruction(NumericBinary(operationName), ScalaStatementAnchor(invocation)))
        return true
      }
    }

    false
  }

  private def tryTransformBinaryRelationalOperator(function: ScSyntheticFunction, invocationInfo: InvocationInfo,
                                                   invocation: ScExpression,
                                                   builder: ScalaDfaControlFlowBuilder): Boolean = {
    for (operationName <- RelationalBinary.keys) {
      if (matchesSignature(function, operationName, ScalaBoolean)) {
        val operation = RelationalBinary(operationName)
        val forceEqualityByContent = operation == RelationType.EQ || operation == RelationType.NE
        val (leftArg, rightArg) = argumentsForBinarySyntheticOperator(invocationInfo)
        transformBinaryOperands(leftArg, rightArg, forceEqualityByContent, builder)

        builder.addInstruction(new BooleanBinaryInstruction(operation, forceEqualityByContent,
          ScalaStatementAnchor(invocation)))
        return true
      }
    }

    false
  }

  private def tryTransformBinaryLogicalOperator(function: ScSyntheticFunction, invocationInfo: InvocationInfo,
                                                invocation: ScExpression,
                                                builder: ScalaDfaControlFlowBuilder): Boolean = {
    for (operationName <- LogicalBinary.keys) {
      if (matchesSignature(function, operationName, ScalaBoolean)) {
        val (leftArg, rightArg) = argumentsForBinarySyntheticOperator(invocationInfo)

        val anchor = ScalaStatementAnchor(invocation)
        val endOffset = new DeferredOffset
        val nextConditionOffset = new DeferredOffset

        leftArg.content.transform(builder)

        val valueNeededToContinue = LogicalBinary(operationName) == LogicalOperation.And
        builder.addInstruction(new ConditionalGotoInstruction(nextConditionOffset,
          DfTypes.booleanValue(valueNeededToContinue)))
        builder.addInstruction(new PushValueInstruction(DfTypes.booleanValue(!valueNeededToContinue), anchor))
        builder.addInstruction(new GotoInstruction(endOffset))

        builder.setOffset(nextConditionOffset)
        builder.addInstruction(new FinishElementInstruction(null))
        rightArg.content.transform(builder)
        builder.setOffset(endOffset)
        builder.addInstruction(new ResultOfInstruction(anchor))
        return true
      }
    }

    false
  }

  private def tryTransformUnaryNumericOperator(function: ScSyntheticFunction, invocationInfo: InvocationInfo,
                                               invocation: ScExpression,
                                               builder: ScalaDfaControlFlowBuilder): Boolean = {
    for (typeClass <- List(ScalaInt, ScalaLong); operationName <- NumericUnary.keys) {
      if (matchesSignature(function, operationName, typeClass)) {
        val singleThisArg = invocationInfo.argListsInEvaluationOrder.head.head
        val returnDfType = scTypeToDfType(function.retType).asInstanceOf[DfIntegralType]

        builder.addInstruction(new PushValueInstruction(returnDfType.meetRange(LongRangeSet.point(0L))))
        singleThisArg.content.transform(builder)

        builder.addInstruction(new NumericBinaryInstruction(NumericUnary(operationName), ScalaStatementAnchor(invocation)))
        return true
      }
    }

    false
  }

  private def tryTransformUnaryLogicalOperator(function: ScSyntheticFunction, invocationInfo: InvocationInfo,
                                               invocation: ScExpression,
                                               builder: ScalaDfaControlFlowBuilder): Boolean = {
    for (operationName <- LogicalUnary.keys) {
      if (matchesSignature(function, operationName, ScalaBoolean)) {
        val singleThisArg = invocationInfo.argListsInEvaluationOrder.head.head
        singleThisArg.content.transform(builder)

        LogicalUnary(operationName) match {
          case LogicalOperation.Not => builder.addInstruction(new NotInstruction(ScalaStatementAnchor(invocation)))
            return true
          case _ =>
        }
      }
    }

    false
  }
}
