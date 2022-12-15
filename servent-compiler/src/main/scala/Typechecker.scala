package edu.harvard.servent.compiler

object Typechecker {
    def typecheck(expression: Expression): (List[Expression], Schema) = {
        val (expressions, schema) = extractSchema(expression)
        val newSchema = extractRequests(schema)
        val (newExpressions, newNewSchema) = extractExpressionRequests(expressions, newSchema)
        newNewSchema.tables.foreach((table) => {
            assert(!table.requests.contains(GetLength()) || table.requests.contains(Get()))
            if (table.requests.contains(Post())) {
                table.columns.map((column) => {
                    assert(table.requests.contains(PostColumn(column.name))
                        || column.default.isDefined
                        || column.t == Void())
                })
            }
        })
        (newExpressions, newNewSchema)
    }

    def extractSchema(expression: Expression): (List[Expression], Schema) = {
        expression match {
            case ExpressionTag("MAIN", Nil, children) => {
                val (expressions, tables) = children.foldLeft((List[Expression](), List[Table]())) { (prev, curr) =>
                    val (prevExpressions, prevTables) = prev
                    val table = extractTable(curr)
                    table.map((t) => {
                        assert(prevTables.find((p) => p.name == t.name).isEmpty)
                        (prevExpressions, t :: prevTables)
                    }).getOrElse((curr :: prevExpressions, prevTables))
                }
                (expressions.reverse, Schema(tables))
            }
            case _ => {
                assert(false)
                null
            }
        }
    }

    def extractTable(expression: Expression): Option[Table] = {
        expression match {
            case ExpressionTag("TABLE", List(Attribute("name", Some(ExpressionString(name)))), children) => {
                val columns = children.map(extractColumn)
                val id = Column(Private(), None, None, None, None, "ID", UUID())
                Some(Table(name, id, columns, Set.empty))
            }
            case _ =>
                None
        }
    }

    def extractColumn(expression: Expression): Column = {
        expression match {
            case ExpressionTag(name, attributes, List(code: ExpressionCode)) =>
                extractAttributes(attributes, Some(code), name)
            case ExpressionTag(name, attributes, List(tag: ExpressionTag)) =>
                extractAttributes(attributes, Some(ExpressionCode(List(CodeTag(tag)))), name)
            case ExpressionTag(name, attributes, Nil) =>
                extractAttributes(attributes, None, name)
            case _ => {
                assert(false)
                null
            }
        }
    }

    def extractAttributes(attributes: List[Attribute], format: Option[ExpressionCode], name: String): Column = {
        val none = Option.empty[ExpressionCode]
        val newAttributes = attributes.foldLeft((Option.empty[Access], none, none, Option.empty[String], Option.empty[Type])) { (prev, curr) =>
            val (prevAccess, prevCondition, prevDefault, prevLabel, prevType) = prev
            curr match {
                case Attribute("condition", Some(code: ExpressionCode)) => {
                    assert(prevCondition.isEmpty)
                    (prevAccess, Some(code), prevDefault, prevLabel, prevType)
                }
                case Attribute("default", Some(code: ExpressionCode)) => {
                    assert(prevDefault.isEmpty)
                    (prevAccess, prevCondition, Some(code), prevLabel, prevType)
                }
                case Attribute("int", None) => {
                    assert(prevType.isEmpty)
                    (prevAccess, prevCondition, prevDefault, prevLabel, Some(Integer()))
                }
                case Attribute("label", Some(ExpressionString(label))) => {
                    assert(prevLabel.isEmpty)
                    (prevAccess, prevCondition, prevDefault, Some(label), prevType)
                }
                case Attribute("long", None) => {
                    assert(prevType.isEmpty)
                    (prevAccess, prevCondition, prevDefault, prevLabel, Some(Long()))
                }
                case Attribute("private", None) => {
                    assert(prevAccess.isEmpty)
                    (Some(Private()), prevCondition, prevDefault, prevLabel, prevType)
                }
                case Attribute("public", None) => {
                    assert(prevAccess.isEmpty)
                    (Some(Public()), prevCondition, prevDefault, prevLabel, prevType)
                }
                case Attribute("string", None) => {
                    assert(prevType.isEmpty)
                    (prevAccess, prevCondition, prevDefault, prevLabel, Some(String_()))
                }
                case Attribute("uuid", None) => {
                    assert(prevType.isEmpty)
                    (prevAccess, prevCondition, prevDefault, prevLabel, Some(UUID()))
                }
                case Attribute("void", None) => {
                    assert(prevType.isEmpty)
                    (prevAccess, prevCondition, prevDefault, prevLabel, Some(Void()))
                }
                case _ => {
                    assert(false)
                    null
                }
            }
        }
        val (access, condition, default, label, t) = newAttributes
        Column(access.getOrElse(Public()), condition, default, format, label, name, t.getOrElse(UUID()))
    }

    def extractRequests(schema: Schema): Schema = {
        schema.tables.foldLeft(schema) { (prev, curr) =>
            extractRequests(curr, prev)
        }
    }

    def extractRequests(table: Table, schema: Schema): Schema = {
        table.columns.foldLeft(schema) { (prev, curr) =>
            val (column, newSchema) = extractRequests(curr, prev)
            val newTables = newSchema.tables.map((newTable) => {
                newTable.name match {
                    case table.name => {
                        val columns = newTable.columns.map((newColumn) => {
                            newColumn.name match {
                                case column.name => column
                                case _ => newColumn
                            }
                        })
                        Table(newTable.name, newTable.id, columns, newTable.requests)
                    }
                    case _ => newTable
                }
            })
            Schema(newTables)
        }
    }

    def extractRequests(column: Column, schema: Schema): (Column, Schema) = {
        val (condition, newSchema) = column.condition.map((c) => {
            val (code, newSchema) = extractRequests(c, schema)
            (Some(code), newSchema)
        }).getOrElse((None, schema))
        val (default, newNewSchema) = column.default.map((d) => {
            val (code, newNewSchema) = extractRequests(d, newSchema)
            (Some(code), newNewSchema)
        }).getOrElse((None, newSchema))
        val (format, newNewNewSchema) = column.format.map((f) => {
            val (code, newNewNewSchema) = extractRequests(f, newNewSchema)
            (Some(code), newNewNewSchema)
        }).getOrElse((None, newNewSchema))
        (Column(column.access, condition, default, format, column.label, column.name, column.t), newNewNewSchema)
    }

    def extractRequests(code: ExpressionCode, schema: Schema): (ExpressionCode, Schema) = {
        val (newCode, tables) = schema.tables.foldLeft((code, List[Table]())) { (prev, curr) =>
            val (prevCode, prevTables) = prev
            val (newCode, newTable) = extractRequests(prevCode, curr)
            (newCode, newTable :: prevTables)
        }
        (newCode, Schema(tables))
    }

    def extractRequests(code: ExpressionCode, table: Table): (ExpressionCode, Table) = {
        code.codes match {
            case (request: CodeRequest) :: tail => {
                val (newCode, newTable) = extractRequests(ExpressionCode(tail), table)
                (ExpressionCode(request :: newCode.codes), newTable)
            }
            case CodeString(string) :: tail => {
                string.indexOf(table.name) match {
                    case -1 => {
                        val (newCode, newTable) = extractRequests(ExpressionCode(tail), table)
                        (ExpressionCode(CodeString(string) :: newCode.codes), newTable)
                    }
                    case index => {
                        val (preRequest, substring) = string.splitAt(index)
                        val indices = List(substring.indexOf("}"), substring.indexOf(" ")).filter((i) => i >= 0)
                        val (request, postRequest) = substring.splitAt(if (indices.length > 0) indices.min else substring.length)
                        val split = request.split("\\.")
                        split.lift(0) match {
                            case Some(table.name) => {
                                assert(2 <= split.length && split.length <= 4)
                                val (newCode, patch) = (split(1), split.lift(2), split.lift(3)) match {
                                    case ("DELETE", None, None) =>
                                        (CodeRequest(table.name, Delete(), None), false)
                                    case ("GET", None, None) =>
                                        (CodeRequest(table.name, Get(), None), false)
                                    case ("GET", Some("LENGTH"), None) =>
                                        (CodeRequest(table.name, GetLength(), None), false)
                                    case ("PATCH", Some(column), Some(_)) =>
                                        (CodeRequest(table.name, PatchColumn(column), split.lift(3)), true)
                                    case ("POST", None, None) =>
                                        (CodeRequest(table.name, Post(), None), false)
                                    case ("POST", Some(column), Some(_)) =>
                                        (CodeRequest(table.name, PostColumn(column), split.lift(3)), false)
                                    case _ => {
                                        assert(false)
                                        null
                                    }
                                }
                                val newCodes = postRequest match {
                                    case "" => tail
                                    case _ => CodeString(postRequest) :: tail
                                }
                                val requests = if (patch) then (table.requests + newCode.request + Patch()) else (table.requests + newCode.request)
                                val newTable = Table(table.name, table.id, table.columns, requests)
                                val (newNewCode, newNewTable) = extractRequests(ExpressionCode(newCodes), newTable)
                                val newNewCodes = preRequest match {
                                    case "" => newCode :: newNewCode.codes
                                    case _ => CodeString(preRequest) :: newCode :: newNewCode.codes
                                }
                                (ExpressionCode(newNewCodes), newNewTable)
                            }
                            case _ => {
                                val (newCode, newTable) = extractRequests(ExpressionCode(tail), table)
                                (ExpressionCode(CodeString(string) :: newCode.codes), newTable)
                            }
                        }
                    }
                }
            }
            case CodeTag(tag) :: tail => {
                val (newTag, schema) = extractRequests(tag, Schema(List(table)))
                newTag match {
                    case t: ExpressionTag => {
                        val (newCode, newTable) = extractRequests(ExpressionCode(tail), schema.tables(0))
                        (ExpressionCode(CodeTag(t) :: newCode.codes), newTable)
                    }
                    case _ => {
                        assert(false)
                        null
                    }
                }
            }
            case Nil =>
                (ExpressionCode(List.empty), table)
        }
    }

    def extractRequests(expression: Expression, schema: Schema): (Expression, Schema) = {
        expression match {
            case code: ExpressionCode =>
                extractRequests(code, schema)
            case ExpressionEndTag(name) =>
                (expression, schema)
            case ExpressionString(string) =>
                (expression, schema)
            case ExpressionTag(name, attributes, children) => {
                val (newAttributes, newSchema) = extractAttributeRequests(attributes, schema)
                val (newChildren, newNewSchema) = extractExpressionRequests(children, newSchema)
                (ExpressionTag(name, newAttributes, newChildren), newNewSchema)
            }
            case ExpressionText(text) =>
                (expression, schema)
        }
    }

    def extractAttributeRequests(attributes: List[Attribute], schema: Schema): (List[Attribute], Schema) = {
        attributes.foldLeft((List[Attribute](), schema)) { (prev, curr) =>
            val (prevAttributes, prevSchema) = prev
            curr match {
                case Attribute(key, Some(code: ExpressionCode)) => {
                    val (newCode, newSchema) = extractRequests(code, prevSchema)
                    (Attribute(key, Some(newCode)) :: prevAttributes, newSchema)
                }
                case _ =>
                    (curr :: prevAttributes, prevSchema)
            }
        }
    }

    def extractExpressionRequests(expressions: List[Expression], schema: Schema): (List[Expression], Schema) = {
        val (newExpressions, newSchema) = expressions.foldLeft((List[Expression](), schema)) { (prev, curr) =>
            val (prevExpressions, prevSchema) = prev
            val (expression, newSchema) = extractRequests(curr, prevSchema)
            (expression :: prevExpressions, newSchema)
        }
        (newExpressions.reverse, newSchema)
    }
}
