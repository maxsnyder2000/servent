package edu.harvard.servent.compiler

object Parser {
    def parse(input: String): Expression = {
        val (expression, postExpression) = parseExpression(input)
        assert(postExpression.isBlank())
        expression
    }

    def parseExpression(input: String): (Expression, String) = {
        val trimmed = input.trim()
        (trimmed(0), trimmed(1)) match {
            case ('{', _) =>
                parseCode(trimmed, 0, 0)
            case ('\"', _) =>
                parseString(trimmed)
            case ('<', '/') =>
                parseEndTag(trimmed)
            case ('<', _) =>
                parseTag(trimmed)
            case _ =>
                parseText(trimmed)
        }
    }

    def parseCode(input: String, index: Int, depth: Int): (ExpressionCode, String) = {
        input(index) match {
            case '{' =>
                parseCode(input, index + 1, depth + 1)
            case '}' => {
                assert(depth > 0)
                depth match {
                    case 1 =>
                        (ExpressionCode(List(CodeString(input.substring(1, index).trim()))), input.substring(index + 1).trim())
                    case _ =>
                        parseCode(input, index + 1, depth - 1)
                }
            }
            case _ =>
                parseCode(input, index + 1, depth)
        }
    }

    def parseEndTag(input: String): (ExpressionEndTag, String) = {
        val index = input.indexOf(">")
        assert(index >= 0)
        (ExpressionEndTag(input.substring(2, index).trim()), input.substring(index + 1).trim())
    }

    def parseString(input: String): (ExpressionString, String) = {
        val index = input.substring(1).indexOf("\"") + 1
        assert(index >= 0)
        (ExpressionString(input.substring(1, index)), input.substring(index + 1).trim())
    }

    def parseTag(input: String): (ExpressionTag, String) = {
        val trimmed = input.substring(1).trim()
        val indices = List(trimmed.indexOf(">"), trimmed.indexOf("/>"), trimmed.indexOf(" ")).filter((i) => i >= 0)
        assert(indices.length > 0)
        val index = indices.min
        val (name, postName) = trimmed.splitAt(index)
        trimmed(index) match {
            case '>' => {
                val (children, postChildren) = parseTagChildren(postName.substring(1).trim(), name)
                (ExpressionTag(name, List.empty, children), postChildren)
            }
            case _ => {
                val (attributes, postAttributes) = parseTagAttributes(postName.substring(1).trim())
                postAttributes.substring(0, 2) match {
                    case "/>" =>
                        (ExpressionTag(name, attributes, List.empty), postAttributes.substring(2).trim())
                    case _ => {
                        val (children, postChildren) = parseTagChildren(postAttributes.substring(1).trim(), name)
                        (ExpressionTag(name, attributes, children), postChildren)
                    }
                }
            }
        }
    }

    def parseTagAttributes(input: String): (List[Attribute], String) = {
        input(0) match {
            case '>' =>
                (List.empty, input)
            case '/' =>
                (List.empty, input)
            case _ => {
                val indices = List(input.indexOf(">"), input.indexOf("="), input.indexOf(" ")).filter((i) => i >= 0)
                assert(indices.length > 0)
                val index = indices.min
                val (key, postKey) = input.splitAt(index)
                val trimmed = postKey.substring(1).trim()
                (input(index), trimmed(0)) match {
                    case ('>', _) =>
                        (List(Attribute(key, None)), postKey)
                    case ('=', _) => {
                        val (expression, postExpression) = parseExpression(trimmed)
                        val (attributes, postAttributes) = parseTagAttributes(postExpression)
                        (Attribute(key, Some(expression)) :: attributes, postAttributes)
                    }
                    case (_, '=') => {
                        val (expression, postExpression) = parseExpression(trimmed.substring(1))
                        val (attributes, postAttributes) = parseTagAttributes(postExpression)
                        (Attribute(key, Some(expression)) :: attributes, postAttributes)
                    }
                    case (_, _) => {
                        val (attributes, postAttributes) = parseTagAttributes(trimmed)
                        (Attribute(key, None) :: attributes, postAttributes)
                    }
                }
            }
        }
    }

    def parseTagChildren(input: String, name: String): (List[Expression], String) = {
        val (expression, postExpression) = parseExpression(input)
        expression match {
            case ExpressionEndTag(name) =>
                (List.empty, postExpression)
            case _ => {
                val (children, postChildren) = parseTagChildren(postExpression, name)
                (expression :: children, postChildren)
            }
        }
    }

    def parseText(input: String): (ExpressionText, String) = {
        val indices = List(input.indexOf("{"), input.indexOf("<")).filter((i) => i >= 0)
        assert(indices.length > 0)
        val index = indices.min
        (ExpressionText(input.substring(0, index).trim()), input.substring(index).trim())
    }
}
