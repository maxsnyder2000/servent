package edu.harvard.servent.compiler

import java.io.File
import java.io.PrintWriter

import scala.concurrent.Future

object Generator {
    def generate(expressions: List[Expression], schema: Schema): Unit = {
        generateServer(schema)
        generateClient(expressions, schema)
    }

    def generateClient(expressions: List[Expression], schema: Schema): Unit = {
        val output = new PrintWriter(new File("../servent-ui/src/App.js"))
        val div = ExpressionTag("div", List(Attribute("className", Some(ExpressionString("App")))), expressions)
        val delete = schema.tables.find((table) => table.requests.contains(Delete())).isDefined
        val get = schema.tables.find((table) => table.requests.contains(Get())).isDefined
        val patch = get && schema.tables.find((table) => table.requests.contains(Patch())).isDefined
        val post = schema.tables.find((table) => table.requests.contains(Post())).isDefined
        output.write(
            "import {useCallback" +
                (if (get) then ", useEffect" else "") +
                (if (get || post) then ", useState" else "") + "} from \"react\";\n" +
            "import {" +
                (if (delete) then "deleteRequest, " else "") +
                (if (get) then "getRequest, " else "") +
                (if (patch) then "patchRequest, " else "") +
                (if (post) then "postRequest" else "") + "} from \"./requests\";\n\n" +
            "import \"./App.css\";\n\n" +
            "const App = () => {\n" +
            indent(generateClientSchema(schema)) + "\n" +
            indent("return (\n" + indent(generateClientExpression(div, schema)) + "\n);") + "\n" +
            "};\n\n" +
            "export default App;\n"
        )
        output.close()
    }

    def generateClientSchema(schema: Schema): String = {
        schema.tables.map((table) => {
            val columns = table.columns.filter((c) => c.t != Void())
            val delete = table.requests.contains(Delete())
            val get = table.requests.contains(Get())
            val patch = get && table.requests.contains(Patch())
            val patchColumns = table.requests.flatMap((request) => {
                request match {
                    case PatchColumn(columnName) => table.columns.find((column) => column.name == columnName)
                    case _ => None
                }
            })
            val post = table.requests.contains(Post())
            val postColumns = table.requests.flatMap((request) => {
                request match {
                    case PostColumn(columnName) => table.columns.find((column) => column.name == columnName)
                    case _ => None
                }
            })
            "// TABLE: " + table.name + "\n" +
            (if (get) then
                "// GET\n" +
                "const [data" + table.name + ", setData" + table.name + "] = useState([]);\n" +
                "const get" + table.name + " = useCallback(() => {\n" +
                indent("getRequest(\"/" + table.name + "/GET\").then((response) => setData" + table.name + "(response));") + "\n" +
                "}, []);\n" +
                "useEffect(get" + table.name + ", [get" + table.name + "]);\n"
            else "") +
            (if (delete) then
                "// DELETE\n" +
                "const delete" + table.name + " = useCallback(() => {\n" +
                indent("deleteRequest(\"/" + table.name + "/DELETE\")" + (if (get) then ".then(get" + table.name + ")" else "") + ";") + "\n" +
                "}, [" + (if (get) then "get" + table.name else "") + "]);\n"
            else "") +
            (if (patch) then
                "// PATCH\n" +
                patchColumns.map((column) => {
                "const patch" + table.name + column.name + " = useCallback((row, i) => {\n" +
                    indent("const body = {\n" +
                    indent("id: row.id,\n" +
                    column.name.toLowerCase() + ": i") + "\n" +
                    "};\n" +
                    "patchRequest(\"/" + table.name + "/PATCH\", body).then(get" + table.name + ");") + "\n" +
                    "}, [get" + table.name + "]);"
                }).mkString("\n") + "\n"
            else "") +
            (if (post) then
                "// POST\n" +
                postColumns.map((column) => {
                    "const [value" + table.name + column.name + ", setValue" + table.name + column.name + "] = useState(\"\");\n" +
                    "const onChange" + table.name + column.name + " = useCallback((event) => {\n" +
                    indent("setValue" + table.name + column.name + "(event?.target?.value);") + "\n" +
                    "}, []);"
                }).mkString("\n") + "\n" +
                "const post" + table.name + " = useCallback(() => {\n" +
                indent(postColumns.flatMap((column) => {
                    column.condition.map((condition) => {
                        "if (!(" + generateCode(condition, schema) + ")(value" + table.name + column.name + ")) {\n" +
                        indent("return;") + "\n" +
                        "}"
                    })
                }).mkString("\n") + "\n" +
                "const body = {\n" +
                indent(columns.map((column) => {
                    column.default match {
                        case Some(default) => column.name.toLowerCase() + ": (" + generateCode(default, schema) + ")()"
                        case _ => column.name.toLowerCase() + ": value" + table.name + column.name
                    }
                }).mkString(",\n")) + "\n" +
                "};\n" +
                "postRequest(\"/" + table.name + "/POST\", body)" + (if (get) then ".then(get" + table.name + ")" else "") + ".then(() => {\n" +
                indent(postColumns.map((column) => {
                    "setValue" + table.name + column.name + "(\"\");"
                }).mkString("\n")) + "\n" +
                "});") + "\n" +
                "}, [" + (if (get) then "get" + table.name + ", " else "") + postColumns.map((column) => {
                    "value" + table.name + column.name
                }).mkString(", ") + "]);\n"
            else "")
        }).mkString("\n")
    }

    def generateClientExpression(expression: Expression, schema: Schema): String = {
        expression match {
            case code: ExpressionCode =>
                "{" + generateCode(code, schema) + "}"
            case ExpressionEndTag(name) =>
                "</" + name + ">"
            case ExpressionString(string) =>
                "\"" + string + "\""
            case ExpressionTag(name, attributes, children) => {
                val attributesString = attributes.map((a) => a.key + a.value.map((v) => "=" + generateClientExpression(v, schema)).getOrElse("")).mkString(" ")
                val childrenString = "\n" + indent(children.map((child) => generateClientExpression(child, schema)).mkString("\n")) + "\n"
                (attributes, children) match {
                    case (Nil, Nil) => "<" + name + " />"
                    case (Nil, _) => "<" + name + ">" + childrenString + "</" + name + ">"
                    case (_, Nil) => "<" + name + " " + attributesString + " />"
                    case (_, _) => "<" + name + " " + attributesString + ">" + childrenString + "</" + name + ">"
                }
            }
            case ExpressionText(text) =>
                text
        }
    }

    def generateServer(schema: Schema): Unit = {
        val root = "../servent-service/src/main/java/edu/harvard/servent/tables/"
        new File(root).listFiles.foreach((directory) => {
            directory.listFiles().foreach((file) => file.delete())
            directory.delete()
        })
        schema.tables.map((table) => {
            val directory = root + table.name.toLowerCase() + "/"
            new File(directory).mkdirs()
            generateServerController(directory, table)
            if (table.requests.contains(Get())) then generateServerGetDTO(directory, table)
            if (table.requests.contains(Get()) && table.requests.contains(Patch())) then generateServerPatchDTO(directory, table)
            if (table.requests.contains(Post())) then generateServerPostDTO(directory, table)
            generateServerRepository(directory, table)
            generateServerRow(directory, table)
            generateServerService(directory, table)
        })
    }

    def generateServerController(directory: String, table: Table): Unit = {
        val output = new PrintWriter(new File(directory + table.name + "Controller.java"))
        val delete = table.requests.contains(Delete())
        val get = table.requests.contains(Get())
        val patch = get && table.requests.contains(Patch())
        val post = table.requests.contains(Post())
        output.write(
            "package edu.harvard.servent.tables." + table.name.toLowerCase() + ";\n\n" +
            (if (get) then "import java.util.List;\n\n" else "") +
            "import org.springframework.beans.factory.annotation.Autowired;\n" +
            "import org.springframework.http.ResponseEntity;\n" +
            (if (delete) then "import org.springframework.web.bind.annotation.DeleteMapping;\n" else "") +
            (if (get) then "import org.springframework.web.bind.annotation.GetMapping;\n" else "") +
            (if (patch) then "import org.springframework.web.bind.annotation.PatchMapping;\n" else "") +
            (if (post) then "import org.springframework.web.bind.annotation.PostMapping;\n" else "") +
            "import org.springframework.web.bind.annotation.RequestBody;\n" +
            "import org.springframework.web.bind.annotation.RequestMapping;\n" +
            "import org.springframework.web.bind.annotation.RestController;\n\n" +
            "import edu.harvard.servent.Response;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/" + table.name + "\")\n" +
            "public class " + table.name + "Controller {\n" +
            indent("@Autowired\n" +
            table.name + "Service service;\n\n" +
            (if (delete) then
                "@DeleteMapping(value = \"/DELETE\")\n" +
                "public ResponseEntity<Response<Void>> delete() {\n" +
                indent("service.delete();\n" +
                "return Response.ok();") + "\n" +
                "}\n\n"
            else "") +
            (if (get) then
                "@GetMapping(value = \"/GET\")\n" +
                "public ResponseEntity<Response<List<" + table.name + "GetDTO>>> get() {\n" +
                indent("List<" + table.name + "GetDTO> dtos = service.get();\n" +
                "return Response.ok(dtos);") + "\n" +
                "}\n\n"
            else "") +
            (if (patch) then
                "@PatchMapping(value = \"/PATCH\")\n" +
                "public ResponseEntity<Response<Void>> patch(@RequestBody " + table.name + "PatchDTO dto) {\n" +
                indent("service.patch(dto);\n" +
                "return Response.ok();") + "\n" +
                "}\n\n"
            else "") +
            (if (post) then
                "@PostMapping(value = \"/POST\")\n" +
                "public ResponseEntity<Response<Void>> post(@RequestBody " + table.name + "PostDTO dto) {\n" +
                indent("service.post(dto);\n" +
                "return Response.ok();") + "\n" +
                "}\n\n"
            else "")) + "\n" +
            "}\n"
        )
        output.close()
    }

    def generateServerGetDTO(directory: String, table: Table): Unit = {
        val output = new PrintWriter(new File(directory + table.name + "GetDTO.java"))
        val columns = table.columns.filter((c) => c.access == Public() && c.t != Void())
        output.write(
            "package edu.harvard.servent.tables." + table.name.toLowerCase() + ";\n\n" +
            "import java.util.List;\n" +
            (if (table.id.t == UUID()) "import java.util.UUID;\n" else "") +
            "import java.util.stream.Collectors;\n\n" +
            "public class " + table.name + "GetDTO {\n" +
            indent("public " + generateType(table.id.t) + " id;\n" +
            columns.map((column) => {
                "public " + generateType(column.t) + " " + column.name.toLowerCase() + ";"
            }).mkString("\n") + "\n\n" +
            "public " + table.name + "GetDTO(" + table.name + "Row row) {\n" +
            indent((table.id.access match {
                case Private() => "this.id = row.idPublic;"
                case Public() => "this.id = row.id;"
            }) + "\n" +
            columns.map((column) => {
                "this." + column.name.toLowerCase() + " = row." + column.name.toLowerCase() + ";"
            }).mkString("\n")) + "\n" +
            "}\n\n" +
            "public static List<" + table.name + "GetDTO> list(List<" + table.name + "Row> rows) {\n" +
            indent("return rows.stream().sorted().map((row) -> new " + table.name + "GetDTO(row)).collect(Collectors.toList());") + "\n" +
            "}") + "\n" +
            "}\n"
        )
        output.close()
    }

    def generateServerPatchDTO(directory: String, table: Table): Unit = {
        val output = new PrintWriter(new File(directory + table.name + "PatchDTO.java"))
        val columns = table.columns.filter((c) => c.access == Public() && c.t != Void())
        output.write(
            "package edu.harvard.servent.tables." + table.name.toLowerCase() + ";\n\n" +
            (if (table.id.t == UUID()) "import java.util.UUID;\n\n" else "") +
            "public class " + table.name + "PatchDTO {\n" +
            indent("public " + generateType(table.id.t) + " id;\n" +
            columns.map((column) => {
                "public " + generateType(column.t) + " " + column.name.toLowerCase() + ";"
            }).mkString("\n") + "\n\n" +
            "public " + table.name + "Row patch(" + table.name + "Row row) {\n" +
            indent(columns.map((column) => {
                "if (" + column.name.toLowerCase() + " != null) row." + column.name.toLowerCase() + " += " + column.name.toLowerCase() + ";"
            }).mkString("\n") + "\n" +
            "return row;") + "\n" +
            "}") + "\n" +
            "}\n"
        )
        output.close()
    }

    def generateServerPostDTO(directory: String, table: Table): Unit = {
        val output = new PrintWriter(new File(directory + table.name + "PostDTO.java"))
        val columns = table.columns.filter((c) => c.t != Void())
        output.write(
            "package edu.harvard.servent.tables." + table.name.toLowerCase() + ";\n\n" +
            (if (table.id.access == Private() && (table.id.t == Integer() || table.id.t == Long())) "import java.util.Random;\n\n" else "") +
            (if (table.id.t == UUID()) "import java.util.UUID;\n\n" else "") +
            "public class " + table.name + "PostDTO {\n" +
            indent(columns.map((column) => {
                "public " + generateType(column.t) + " " + column.name.toLowerCase() + ";"
            }).mkString("\n") + "\n\n" +
            "public " + table.name + "Row row() {\n" +
            indent(table.name + "Row row = new " + table.name + "Row();\n" +
            (if (table.id.access == Private()) then table.id.t match {
                case Integer() => "row.idPublic = new Random().nextInt();\n"
                case Long() => "row.idPublic = Long.valueOf(new Random().nextInt());\n"
                case UUID() => "row.idPublic = UUID.randomUUID();\n"
                case _ => ""
            } else "") +
            columns.map((column) => {
                "row." + column.name.toLowerCase() + " = " + column.name.toLowerCase() + ";"
            }).mkString("\n") + "\n" +
            "return row;") + "\n" +
            "}") + "\n" +
            "}\n"
        )
        output.close()
    }

    def generateServerRepository(directory: String, table: Table): Unit = {
        val output = new PrintWriter(new File(directory + table.name + "Repository.java"))
        output.write(
            "package edu.harvard.servent.tables." + table.name.toLowerCase() + ";\n\n" +
            (if (table.id.t == UUID()) "import java.util.UUID;\n\n" else "") +
            "import org.springframework.data.jpa.repository.JpaRepository;\n\n" +
            "public interface " + table.name + "Repository extends JpaRepository<" + table.name + "Row, " + generateType(table.id.t) + "> {\n" +
            (if (table.id.access == Private()) then indent(table.name + "Row findByIdPublic(" + generateType(table.id.t) + " idPublic);") + "\n" else "") +
            "}\n"
        )
        output.close()
    }

    def generateServerRow(directory: String, table: Table): Unit = {
        val output = new PrintWriter(new File(directory + table.name + "Row.java"))
        val columns = table.columns.filter((c) => c.t != Void())
        output.write(
            "package edu.harvard.servent.tables." + table.name.toLowerCase() + ";\n\n" +
            "import java.sql.Timestamp;\n" +
            (if (table.id.t == UUID()) "import java.util.UUID;\n" else "") + "\n" +
            "import javax.persistence.Column;\n" +
            "import javax.persistence.Entity;\n" +
            "import javax.persistence.GeneratedValue;\n" +
            "import javax.persistence.GenerationType;\n" +
            "import javax.persistence.Id;\n" +
            "import javax.persistence.Table;\n\n" +
            "import org.hibernate.annotations.CreationTimestamp;\n\n" +
            "@Entity\n" +
            "@Table(name = \"" + table.name + "\")\n" +
            "public class " + table.name + "Row implements Comparable<" + table.name + "Row> {\n" +
            indent("@Id\n" +
            "@GeneratedValue(strategy = GenerationType.AUTO)\n" +
            "public " + generateType(table.id.t) + " id;\n\n" +
            (if (table.id.access == Private()) then
                "@Column(name = \"idPublic\")\n" +
                "public " + generateType(table.id.t) + " idPublic;\n\n"
            else "") +
            columns.map((column) => {
                "@Column(name = \"" + column.name + "\")\n" +
                "public " + generateType(column.t) + " " + column.name.toLowerCase() +";"
            }).mkString("\n\n") + "\n\n" +
            "@CreationTimestamp\n" +
            "private Timestamp creationTimestamp;\n\n" +
            "@Override\n" +
            "public int compareTo(" + table.name + "Row " + "row) {\n" +
            indent("return this.creationTimestamp.compareTo(row.creationTimestamp);") + "\n" +
            "}") + "\n" +
            "}\n"
        )
        output.close()
    }

    def generateServerService(directory: String, table: Table): Unit = {
        val output = new PrintWriter(new File(directory + table.name + "Service.java"))
        val delete = table.requests.contains(Delete())
        val get = table.requests.contains(Get())
        val patch = get && table.requests.contains(Patch())
        val post = table.requests.contains(Post())
        output.write(
            "package edu.harvard.servent.tables." + table.name.toLowerCase() + ";\n\n" +
            (if (get) then "import java.util.List;\n\n" else "") +
            "import org.springframework.beans.factory.annotation.Autowired;\n" +
            "import org.springframework.stereotype.Service;\n\n" +
            "@Service\n" +
            "public class " + table.name + "Service {\n" +
            indent("@Autowired\n" +
            table.name + "Repository repository;\n\n" +
            (if (delete) then
                "public void delete() {\n" +
                indent("repository.deleteAll();") + "\n" +
                "}\n\n"
            else "") +
            (if (get) then
                "public List<" + table.name + "GetDTO> get() {\n" +
                indent("return " + table.name + "GetDTO.list(repository.findAll());") + "\n" +
                "}\n\n"
            else "") +
            (if (patch) then
                "public void patch(" + table.name + "PatchDTO dto) {\n" +
                indent(table.id.access match {
                    case Private() => "repository.save(dto.patch(repository.findByIdPublic(dto.id)));"
                    case Public() => "repository.save(dto.patch(repository.findById(dto.id).get()));"
                }) + "\n" +
                "}\n\n"
            else "") +
            (if (post) then
                "public void post(" + table.name + "PostDTO dto) {\n" +
                indent("repository.save(dto.row());") + "\n" +
                "}\n\n"
            else "")) + "\n" +
            "}\n"
        )
        output.close()
    }

    def generateCode(expression: ExpressionCode, schema: Schema): String = {
        expression.codes.map((code) => {
            code match {
                case CodeRequest(tableName, request, argument) => {
                    val table = schema.tables.filter((t) => t.name == tableName)(0)
                    val columns = table.columns.filter((c) => c.access == Public())
                    (request, argument) match {
                        case (Delete(), None) =>
                            "delete" + table.name
                        case (Get(), None) => {
                            "\n" +
                            indent("<table>\n" +
                            indent("<thead>\n" +
                            indent("<tr>\n" +
                            columns.map((column) => {
                                indent("<th>" + column.label.getOrElse(column.name) + "</th>")
                            }).mkString("\n") + "\n" +
                            "</tr>") + "\n" +
                            "</thead>") + "\n" +
                            indent("<tbody>\n" +
                            indent("{data" + table.name + ".map((row) => (\n" +
                            indent("<tr key={row.id}>\n" +
                            columns.map((column) => {
                                val data = column.format match {
                                    case Some(ExpressionCode(List(CodeTag(tag: ExpressionTag)))) =>
                                        "\n" + generateCode(ExpressionCode(List(CodeTag(tag), CodeString("\n"))), schema)
                                    case Some(code: ExpressionCode) =>
                                        "{(" + generateCode(code, schema) + ")(row." + column.name.toLowerCase() + ")}"
                                    case _ =>
                                        "{row." + column.name.toLowerCase() + "}"
                                }
                                indent("<td>" + data + "</td>")
                            }).mkString("\n") + "\n" +
                            "</tr>") + "\n" +
                            "))}") + "\n" +
                            "</tbody>") + "\n" +
                            "</table>") + "\n"
                        }
                        case (GetLength(), None) =>
                            "data" + table.name + ".length"
                        case (PatchColumn(column), Some("INC")) =>
                            "() => patch" + table.name + column + "(row, 1)"
                        case (PatchColumn(column), Some("DEC")) =>
                            "() => patch" + table.name + column + "(row, -1)"
                        case (Post(), None) =>
                            "post" + table.name
                        case (PostColumn(column), Some(attribute)) =>
                            attribute + table.name + column
                        case (_, _) => {
                            assert(false)
                            null
                        }
                    }
                }
                case CodeString(string) =>
                    string
                case CodeTag(tag) =>
                    indent(generateClientExpression(tag, schema))
            }
        }).mkString("")
    }

    def generateType(t: Type): String = {
        t match {
            case Integer() => "Integer"
            case Long() => "Long"
            case String_() => "String"
            case UUID() => "UUID"
            case Void() => {
                assert(false)
                null
            }
        }
    }

    def indent(string: String): String = string.split("\n").map((line) => if (line.isBlank()) then "" else "\t" + line).mkString("\n")
}
