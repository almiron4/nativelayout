/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

package co.adilson889.nativelayout.core

class ExpandedScreen(val html: String, val lineMap: List<Int>)

class ExpandError(
    message: String,
    line: Int,
    source: String,
    word: String? = null,
    suggestion: String? = null
) : NativeLayoutError(message, line, extractLine(source, line), word, suggestion)

object Expander {
    fun expand(
        source: String,
        data: Map<String, Any?>,
        resolve: (String) -> String? = { null }
    ): ExpandedScreen = ScreenExpander(source, data, resolve).expand()
}

private const val KIND_TAG = 1
private const val KIND_CLASS = 2
private const val KIND_ID = 3

private class Rule(
    val kind: Int,
    val name: String,
    val declarations: List<Pair<String, String>>,
    val order: Int,
    val line: Int,
    val file: String?,
    val linkLine: Int
) {
    var resolved: List<Pair<String, String>> = emptyList()
}

private class Attr(val key: String, val value: String?)

private class Scope(val parent: Scope?, val vars: Map<String, Any?>) {
    fun has(name: String): Boolean = vars.containsKey(name) || (parent?.has(name) ?: false)
    fun get(name: String): Any? = if (vars.containsKey(name)) vars[name] else parent?.get(name)
}

private sealed class Node
private class TextNode(val text: String, val line: Int) : Node()
private class Branch(val condition: String?, val line: Int) {
    val nodes = ArrayList<Node>()
}
private class IfNode(val line: Int) : Node() {
    val branches = ArrayList<Branch>()
}
private class ForNode(val line: Int, val variable: String, val listName: String) : Node() {
    val nodes = ArrayList<Node>()
}
private class Frame(val openLine: Int, var nodes: MutableList<Node>, val ifNode: IfNode?)
private class Control(val type: String, val argument: String, val variable: String = "")

private class ScreenExpander(
    private val source: String,
    private val data: Map<String, Any?>,
    private val resolve: (String) -> String?
) {

    private val lines = source.split("\n")
    private val rules = ArrayList<Rule>()
    private val variables = LinkedHashMap<String, String>()
    private var ruleOrder = 0
    private val out = ArrayList<String>()
    private val outLines = ArrayList<Int>()

    private val tagRegex = Regex("""<[^<>]*>""")
    private val tagNameRegex = Regex("""^<([A-Za-z][A-Za-z0-9-]*)""")
    private val attrRegex = Regex("""([A-Za-z_:][-A-Za-z0-9_:.]*)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+)))?""")
    private val braceRegex = Regex("""\\[{}]|\{([^{}]*)\}""")
    private val nameRegex = Regex("""^[A-Za-z_][A-Za-z0-9_.]*$""")
    private val singleValueRegex = Regex("""^\{\s*([A-Za-z_][A-Za-z0-9_.]*)\s*\}$""")
    private val varRegex = Regex("""var\(\s*(--[A-Za-z0-9_-]+)\s*\)""")
    private val ruleRegex = Regex("""([^{}]+)\{([^{}]*)\}""")
    private val commentRegex = Regex("""/\*[\s\S]*?\*/""")
    private val forRegex = Regex("""^for\s+([A-Za-z_][A-Za-z0-9_]*)\s+in\s+([A-Za-z_][A-Za-z0-9_.]*):$""")
    private val keywordRegex = Regex("""^(if|elif|for)\s""")
    private val elseSpaceRegex = Regex("""^else\s+:$""")
    private val tokenRegex = Regex("""==|!=|<=|>=|<|>|\(|\)|"[^"]*"|-?\d+(?:\.\d+)?|[A-Za-z_][A-Za-z0-9_.]*""")
    private val tagSelector = Regex("""[A-Za-z][A-Za-z0-9]*""")
    private val classSelector = Regex("""\.[A-Za-z_][A-Za-z0-9_-]*""")
    private val idSelector = Regex("""#[A-Za-z_][A-Za-z0-9_-]*""")
    private val comparators = setOf("==", "!=", "<", ">", "<=", ">=")

    private fun err(message: String, line: Int, word: String? = null, suggestion: String? = null) =
        ExpandError(message, line, source, word, suggestion)

    private fun styleError(message: String, line: Int, word: String?, file: String?, linkLine: Int): ExpandError =
        if (file == null) err("style: $message", line, word)
        else err("style: in '$file' line $line: $message", linkLine, word)

    private fun linkHref(tag: String, line: Int): String {
        val attrs = HashMap<String, String>()
        val inner = tag.removePrefix("<link").removeSuffix(">").removeSuffix("/")
        for (a in attrRegex.findAll(inner)) {
            val value = a.groups[2]?.value ?: a.groups[3]?.value ?: a.groups[4]?.value ?: continue
            attrs[a.groupValues[1].lowercase()] = value
        }
        val href = attrs["href"]
        if (href == null || href.isBlank() || attrs["rel"]?.lowercase() != "stylesheet") {
            throw err("markup: <link> needs rel=\"stylesheet\" and a href", line)
        }
        if (href.startsWith("/") || href.contains("..") || href.contains("://")) {
            throw err("style: the file '$href' is not allowed", line, href)
        }
        if (!href.endsWith(".css")) throw err("style: the file '$href' must be a .css file", line, href)
        return href
    }

    fun expand(): ExpandedScreen {
        var first = 0
        while (first < lines.size && lines[first].isBlank()) first++
        var last = lines.size - 1
        while (last >= 0 && lines[last].isBlank()) last--
        if (first >= last || lines[first].trim() != "<nativelayout>" || lines[last].trim() != "</nativelayout>") {
            throw err("the file must start with <nativelayout> and end with </nativelayout>", if (first < lines.size) first + 1 else 1)
        }

        var styleStart = -1
        var styleEnd = -1
        var bodyStart = -1
        var bodyEnd = -1
        val links = ArrayList<Pair<String, Int>>()
        var i = first + 1
        var inComment = false
        while (i < last) {
            val t = lines[i].trim()
            when {
                inComment -> {
                    if (t.contains("-->")) inComment = false
                }
                t.isEmpty() -> {
                }
                t.startsWith("<!--") -> {
                    if (!t.contains("-->")) inComment = true
                }
                t.startsWith("<script") -> throw err("markup: <script> is not allowed", i + 1)
                t == "<style>" -> {
                    if (styleStart >= 0) throw err("only one <style> block is allowed", i + 1)
                    var j = i + 1
                    while (j < last && lines[j].trim() != "</style>") j++
                    if (j >= last) throw err("<style> is never closed with </style>", i + 1)
                    styleStart = i + 1
                    styleEnd = j
                    i = j
                }
                t.startsWith("<link") -> {
                    if (bodyStart >= 0) throw err("a <link> must come before <body>", i + 1)
                    links.add(Pair(linkHref(t, i + 1), i + 1))
                }
                t.startsWith("<body") -> {
                    if (bodyStart >= 0) throw err("the file must have exactly one <body>", i + 1)
                    var j = i + 1
                    while (j < last && lines[j].trim() != "</body>") j++
                    if (j >= last) throw err("<body> is never closed with </body>", i + 1)
                    bodyStart = i
                    bodyEnd = j
                    i = j
                }
                else -> throw err("only <style> and <body> are allowed inside <nativelayout>", i + 1)
            }
            i++
        }
        if (bodyStart < 0) throw err("the file must have exactly one <body>", first + 1)

        val loaded = HashSet<String>()
        for ((href, linkLine) in links) {
            if (!loaded.add(href)) continue
            val css = resolve(href) ?: throw err("style: the file '$href' was not found", linkLine, href)
            if (css.contains("@import")) throw err("style: a style file cannot import another file", linkLine, href)
            parseStyle(css, 1, href, linkLine)
        }
        if (styleStart >= 0) parseStyle(lines.subList(styleStart, styleEnd).joinToString("\n"), styleStart + 1)
        resolveVariables()
        val nodes = buildNodes(bodyStart, bodyEnd)
        emit(nodes, Scope(null, data))
        return ExpandedScreen(out.joinToString("\n"), outLines)
    }

    private fun parseStyle(text: String, firstLine: Int, file: String? = null, linkLine: Int = 0) {
        val clean = commentRegex.replace(text) { m -> m.value.filter { it == '\n' } }
        for (m in ruleRegex.findAll(clean)) {
            val selectorGroup = m.groups[1]!!
            val selectorText = selectorGroup.value
            val offset = selectorGroup.range.first + (selectorText.length - selectorText.trimStart().length)
            val line = firstLine + clean.substring(0, offset).count { it == '\n' }
            val declarations = ArrayList<Pair<String, String>>()
            for (part in m.groupValues[2].split(";")) {
                val idx = part.indexOf(':')
                if (idx < 0) continue
                val name = part.substring(0, idx).trim().lowercase()
                val value = part.substring(idx + 1).replace("!important", "").trim()
                if (name.isEmpty() || value.isEmpty()) continue
                declarations.add(Pair(name, value))
            }
            for (raw in selectorText.split(",")) {
                val selector = raw.trim()
                when {
                    selector == ":root" -> {
                        for (d in declarations) if (d.first.startsWith("--")) variables[d.first] = d.second
                    }
                    tagSelector.matches(selector) -> rules.add(Rule(KIND_TAG, selector.lowercase(), declarations, ruleOrder++, line, file, linkLine))
                    classSelector.matches(selector) -> rules.add(Rule(KIND_CLASS, selector.substring(1), declarations, ruleOrder++, line, file, linkLine))
                    idSelector.matches(selector) -> rules.add(Rule(KIND_ID, selector.substring(1), declarations, ruleOrder++, line, file, linkLine))
                    else -> throw styleError("the selector '$selector' is not supported", line, selector, file, linkLine)
                }
            }
        }
    }

    private fun substituteVars(value: String, rule: Rule): String =
        varRegex.replace(value) { m ->
            variables[m.groupValues[1]]
                ?: throw styleError("the variable '${m.groupValues[1]}' is not defined in ':root'", rule.line, m.groupValues[1], rule.file, rule.linkLine)
        }

    private fun resolveVariables() {
        for (rule in rules) {
            val resolved = ArrayList<Pair<String, String>>()
            for (d in rule.declarations) {
                if (d.first.startsWith("--")) continue
                resolved.add(Pair(d.first, substituteVars(d.second, rule)))
            }
            rule.resolved = resolved
        }
    }

    private fun parseControl(t: String, line: Int): Control? {
        if (t == "end") return Control("end", "")
        if (t == "else:") return Control("else", "")
        if (elseSpaceRegex.matches(t)) throw err("write 'else:' without a space before ':'", line)
        val keyword = keywordRegex.find(t)?.groupValues?.get(1) ?: return null
        if (!t.endsWith(":")) return null
        val body = t.substring(0, t.length - 1)
        if (body.endsWith(" ") || body.endsWith("\t")) {
            throw err("write '${body.trimEnd()}:' without a space before ':'", line)
        }
        if (keyword == "for") {
            val m = forRegex.find(t) ?: throw err("invalid 'for': '$t'", line, null, "write 'for item in list:'")
            return Control("for", m.groupValues[2], m.groupValues[1])
        }
        val condition = body.substring(keyword.length).trim()
        if (condition.isEmpty()) throw err("invalid condition: ''", line)
        return Control(keyword, condition)
    }

    private fun buildNodes(from: Int, to: Int): List<Node> {
        val root = ArrayList<Node>()
        val stack = ArrayList<Frame>()
        stack.add(Frame(0, root, null))
        for (idx in from..to) {
            val line = idx + 1
            val control = if (idx == from || idx == to) null else parseControl(lines[idx].trim(), line)
            val top = stack[stack.size - 1]
            if (control == null) {
                top.nodes.add(TextNode(lines[idx], line))
                continue
            }
            when (control.type) {
                "if" -> {
                    val node = IfNode(line)
                    val branch = Branch(control.argument, line)
                    node.branches.add(branch)
                    top.nodes.add(node)
                    stack.add(Frame(line, branch.nodes, node))
                }
                "elif", "else" -> {
                    val ifNode = top.ifNode ?: throw err("'${control.type}' has no matching 'if'", line)
                    if (ifNode.branches[ifNode.branches.size - 1].condition == null) {
                        throw err("'${control.type}' has no matching 'if'", line)
                    }
                    val branch = Branch(if (control.type == "else") null else control.argument, line)
                    ifNode.branches.add(branch)
                    top.nodes = branch.nodes
                }
                "for" -> {
                    val node = ForNode(line, control.variable, control.argument)
                    top.nodes.add(node)
                    stack.add(Frame(line, node.nodes, null))
                }
                else -> {
                    if (stack.size == 1) throw err("'end' has no matching 'if' or 'for'", line)
                    stack.removeAt(stack.size - 1)
                }
            }
        }
        if (stack.size > 1) {
            val open = stack[stack.size - 1]
            val word = if (open.ifNode != null) "if" else "for"
            throw err("the '$word' opened on line ${open.openLine} is never closed with 'end'", open.openLine)
        }
        return root
    }

    private fun emit(nodes: List<Node>, scope: Scope) {
        for (node in nodes) {
            when (node) {
                is TextNode -> {
                    out.add(renderLine(node.text, node.line, scope))
                    outLines.add(node.line)
                }
                is IfNode -> {
                    for (branch in node.branches) {
                        val condition = branch.condition
                        if (condition == null || evaluate(condition, branch.line, scope)) {
                            emit(branch.nodes, scope)
                            break
                        }
                    }
                }
                is ForNode -> {
                    val list = lookup(node.listName, node.line, scope)
                    if (list !is List<*>) throw err("'${node.listName}' is not a list", node.line, node.listName)
                    if (scope.has(node.variable)) throw err("the name '${node.variable}' already exists", node.line, node.variable)
                    for (item in list) emit(node.nodes, Scope(scope, mapOf(node.variable to item)))
                }
            }
        }
    }

    private fun lookup(name: String, line: Int, scope: Scope): Any? {
        val parts = name.split(".")
        if (!scope.has(parts[0])) throw err("the name '$name' was not given by the program", line, parts[0])
        var value: Any? = scope.get(parts[0])
        for (k in 1 until parts.size) {
            val part = parts[k]
            val current = value
            value = when {
                current is Map<*, *> && current.containsKey(part) -> current[part]
                part == "size" && current is Map<*, *> -> current.size
                part == "size" && current is List<*> -> current.size
                part == "size" && current is String -> current.length
                else -> throw err("the name '$name' was not given by the program", line, parts[0])
            }
        }
        return value
    }

    private fun tokenize(text: String, line: Int): List<String> {
        val tokens = ArrayList<String>()
        var pos = 0
        while (pos < text.length) {
            if (text[pos].isWhitespace()) {
                pos++
                continue
            }
            val m = tokenRegex.find(text, pos)
            if (m == null || m.range.first != pos) throw err("invalid condition: '$text'", line)
            tokens.add(m.value)
            pos = m.range.last + 1
        }
        return tokens
    }

    private fun evaluate(condition: String, line: Int, scope: Scope): Boolean =
        ConditionParser(tokenize(condition, line), condition, line, scope).run()

    private inner class ConditionParser(
        val tokens: List<String>,
        val text: String,
        val line: Int,
        val scope: Scope
    ) {
        var pos = 0

        fun run(): Boolean {
            val value = parseOr()
            if (pos != tokens.size) throw bad()
            return value
        }

        fun bad(): ExpandError = err("invalid condition: '$text'", line)

        fun peek(): String? = if (pos < tokens.size) tokens[pos] else null

        fun parseOr(): Boolean {
            var v = parseAnd()
            while (peek() == "or") {
                pos++
                val r = parseAnd()
                v = v || r
            }
            return v
        }

        fun parseAnd(): Boolean {
            var v = parseNot()
            while (peek() == "and") {
                pos++
                val r = parseNot()
                v = v && r
            }
            return v
        }

        fun parseNot(): Boolean {
            if (peek() == "not") {
                pos++
                return !parseNot()
            }
            if (peek() == "(") {
                pos++
                val v = parseOr()
                if (peek() != ")") throw bad()
                pos++
                return v
            }
            return parseCompare()
        }

        fun parseCompare(): Boolean {
            val left = operand()
            val op = peek()
            if (op != null && op in comparators) {
                pos++
                val right = operand()
                return compare(op, left, right)
            }
            if (left is Boolean) return left
            throw err("invalid condition: '$text'", line, null, "a name alone must be true or false")
        }

        fun operand(): Any? {
            val token = peek() ?: throw bad()
            pos++
            if (token == "true") return true
            if (token == "false") return false
            if (token.startsWith("\"")) return token.substring(1, token.length - 1)
            if (token[0].isDigit() || token[0] == '-') return token.toDouble()
            if (token == "and" || token == "or" || token == "not" || token == "(" || token == ")" || token in comparators) throw bad()
            return lookup(token, line, scope)
        }

        fun compare(op: String, a: Any?, b: Any?): Boolean {
            val x = (a as? Number)?.toDouble()
            val y = (b as? Number)?.toDouble()
            if (x != null && y != null) {
                return when (op) {
                    "==" -> x == y
                    "!=" -> x != y
                    "<" -> x < y
                    ">" -> x > y
                    "<=" -> x <= y
                    else -> x >= y
                }
            }
            if (op == "==") return a == b
            if (op == "!=") return a != b
            if (a is String && b is String) {
                val c = a.compareTo(b)
                return when (op) {
                    "<" -> c < 0
                    ">" -> c > 0
                    "<=" -> c <= 0
                    else -> c >= 0
                }
            }
            throw err("invalid condition: '$text'", line, null, "these values cannot be compared with '$op'")
        }
    }

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun formatNumber(n: Number): String {
        if (n is Double || n is Float) {
            val d = n.toDouble()
            if (!d.isInfinite() && !d.isNaN() && d == Math.floor(d)) return d.toLong().toString()
            return d.toString()
        }
        return n.toString()
    }

    private fun singular(name: String): String {
        val last = name.substringAfterLast('.')
        return if (last.length > 1 && last.endsWith("s")) last.dropLast(1) else "item"
    }

    private fun valueText(inner: String, line: Int, scope: Scope): String {
        val name = inner.trim()
        if (!nameRegex.matches(name)) {
            throw err("arithmetic is not supported in '{$inner}'; give the program's result as a name", line)
        }
        return when (val v = lookup(name, line, scope)) {
            null -> ""
            is String -> escapeHtml(v)
            is Boolean -> v.toString()
            is Number -> formatNumber(v)
            is List<*> -> throw err("'{$name}' is a list; use 'for ${singular(name)} in $name:'", line, name)
            is Map<*, *> -> throw err("'{$name}' is a map; use a name inside it, like '{$name.field}'", line, name)
            else -> escapeHtml(v.toString())
        }
    }

    private fun renderText(text: String, line: Int, scope: Scope): String =
        braceRegex.replace(text) { m ->
            when (m.value) {
                "\\{" -> "{"
                "\\}" -> "}"
                else -> valueText(m.groupValues[1], line, scope)
            }
        }

    private fun renderLine(text: String, line: Int, scope: Scope): String {
        val sb = StringBuilder()
        var pos = 0
        for (m in tagRegex.findAll(text)) {
            sb.append(renderText(text.substring(pos, m.range.first), line, scope))
            sb.append(renderTag(m.value, line, scope))
            pos = m.range.last + 1
        }
        sb.append(renderText(text.substring(pos), line, scope))
        return sb.toString()
    }

    private fun styleFor(tag: String, classes: List<String>, id: String?): String {
        val matched = rules.filter {
            when (it.kind) {
                KIND_TAG -> it.name == tag
                KIND_CLASS -> it.name in classes
                else -> id != null && it.name == id
            }
        }.sortedWith(compareBy<Rule>({ it.kind }, { it.order }))
        val parts = ArrayList<String>()
        for (rule in matched) for (d in rule.resolved) parts.add(d.first + ": " + d.second)
        return parts.joinToString("; ")
    }

    private fun renderTag(tag: String, line: Int, scope: Scope): String {
        if (tag.startsWith("<!")) return tag
        if (tag.startsWith("</")) {
            val closing = tag.substring(2, tag.length - 1).trim().lowercase()
            return if (closing == "body") "</div>" else tag
        }
        val nameMatch = tagNameRegex.find(tag) ?: return tag
        val name = nameMatch.groupValues[1]
        val lower = name.lowercase()
        val selfClose = tag.endsWith("/>")
        val inner = tag.substring(nameMatch.range.last + 1, tag.length - (if (selfClose) 2 else 1))

        val result = ArrayList<Attr>()
        var inline = ""
        var classes: List<String> = emptyList()
        var id: String? = null
        for (a in attrRegex.findAll(inner)) {
            val key = a.groupValues[1].lowercase()
            var value: String? = a.groups[2]?.value ?: a.groups[3]?.value ?: a.groups[4]?.value
            if (value != null && value.contains("{")) {
                if (key == "id" || key == "class") throw err("markup: '$key' cannot be generated by value", line, key)
                val single = singleValueRegex.find(value.trim())
                if ((key == "checked" || key == "disabled" || key == "readonly") && single != null) {
                    val flag = lookup(single.groupValues[1], line, scope)
                    if (flag == true) result.add(Attr(a.groupValues[1], "true"))
                    continue
                }
                value = renderText(value, line, scope)
            }
            when (key) {
                "style" -> inline = value ?: ""
                "class" -> {
                    classes = (value ?: "").split(" ").filter { it.isNotEmpty() }
                    result.add(Attr(a.groupValues[1], value))
                }
                "id" -> {
                    id = value
                    result.add(Attr(a.groupValues[1], value))
                }
                else -> result.add(Attr(a.groupValues[1], value))
            }
        }

        val parts = ArrayList<String>()
        val ruleStyle = styleFor(lower, classes, id)
        if (ruleStyle.isNotEmpty()) parts.add(ruleStyle)
        val inlineStyle = inline.trim().trimEnd(';').trim()
        if (inlineStyle.isNotEmpty()) parts.add(inlineStyle)
        if (parts.isNotEmpty()) result.add(Attr("style", parts.joinToString("; ")))

        val sb = StringBuilder("<")
        sb.append(if (lower == "body") "div" else name)
        for (a in result) {
            sb.append(' ').append(a.key)
            val v = a.value
            if (v != null) sb.append("=\"").append(v.replace("\"", "&quot;")).append('"')
        }
        sb.append(if (selfClose) " />" else ">")
        return sb.toString()
    }
}
