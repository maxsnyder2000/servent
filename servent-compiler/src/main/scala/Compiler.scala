package edu.harvard.servent.compiler

import scala.io.Source

object Compiler extends App {
    val input = Source.fromFile("../MAIN.js").mkString
    val expression = Parser.parse(input)
    val (expressions, schema) = Typechecker.typecheck(expression)
    Generator.generate(expressions, schema)
}
