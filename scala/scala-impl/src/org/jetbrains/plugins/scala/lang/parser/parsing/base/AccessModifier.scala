package org.jetbrains.plugins.scala.lang.parser.parsing.base

import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType
import org.jetbrains.plugins.scala.lang.parser.parsing.ParsingRule
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder

/*
 *  AccessModifier ::= ( 'private' | 'protected' ) [ AccessQualifier ]
 */
object AccessModifier extends ParsingRule {
  override def parse(implicit builder: ScalaPsiBuilder): Boolean = {
    val marker = builder.mark()
    builder.getTokenType match {
      case ScalaTokenTypes.kPRIVATE | ScalaTokenTypes.kPROTECTED =>
        builder.advanceLexer() // Ate modifier
        AccessQualifier()
        marker.done(ScalaElementType.ACCESS_MODIFIER)
        true
      case _ =>
        marker.drop()
        false
    }
  }
}