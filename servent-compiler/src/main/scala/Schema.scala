package edu.harvard.servent.compiler

sealed abstract class Access
case class Private() extends Access
case class Public() extends Access

sealed abstract class Request
case class Delete() extends Request
case class Get() extends Request
case class GetLength() extends Request
case class Patch() extends Request
case class PatchColumn(column: String) extends Request
case class Post() extends Request
case class PostColumn(column: String) extends Request

sealed abstract class Type
case class Integer() extends Type
case class Long() extends Type
case class String_() extends Type
case class UUID() extends Type
case class Void() extends Type

case class Column(
    access: Access,
    condition: Option[ExpressionCode],
    default: Option[ExpressionCode],
    format: Option[ExpressionCode],
    label: Option[String],
    name: String,
    t: Type
)

case class Table(name: String, id: Column, columns: List[Column], requests: Set[Request])

case class Schema(tables: List[Table])
