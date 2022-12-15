package edu.harvard.servent.compiler

case class Attribute(key: String, value: Option[Expression])

sealed abstract class Code
sealed abstract class Expression

case class ExpressionCode(codes: List[Code]) extends Expression
case class ExpressionEndTag(name: String) extends Expression
case class ExpressionString(string: String) extends Expression
case class ExpressionTag(name: String, attributes: List[Attribute], children: List[Expression]) extends Expression
case class ExpressionText(text: String) extends Expression

case class CodeRequest(table: String, request: Request, argument: Option[String]) extends Code
case class CodeString(string: String) extends Code
case class CodeTag(tag: ExpressionTag) extends Code
