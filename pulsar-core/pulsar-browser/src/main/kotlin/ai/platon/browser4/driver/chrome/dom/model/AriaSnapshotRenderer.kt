package ai.platon.browser4.driver.chrome.dom.model

import java.util.LinkedHashMap
import java.util.Locale

object AriaSnapshotRenderer {
    private val yamlKeywords = setOf("null", "true", "false", "~")
    private val whitespaceRegex = Regex("\\s+")

    fun render(root: NanoDOMTreeNode): String {
        val lines = mutableListOf<String>()
        toRenderNode(root)?.let { visitNode(it, "", lines) }
        return lines.joinToString("\n")
    }

    private fun visitNode(node: RenderNode, indent: String, lines: MutableList<String>) {
        val key = indent + "- " + yamlEscapeKeyIfNeeded(createKey(node))
        val singleInlinedTextChild = node.singleInlinedTextChild()

        when {
            node.children.isEmpty() && node.props.isEmpty() -> {
                lines += key
            }

            singleInlinedTextChild != null -> {
                lines += "$key: ${yamlEscapeValueIfNeeded(singleInlinedTextChild)}"
            }

            else -> {
                lines += "$key:"
                node.props.forEach { (name, value) ->
                    lines += "$indent  - /$name: ${yamlEscapeValueIfNeeded(value)}"
                }
                val childIndent = "$indent  "
                node.children.forEach { child ->
                    when (child) {
                        is RenderChild.Text -> lines += "$childIndent- text: ${yamlEscapeValueIfNeeded(child.value)}"
                        is RenderChild.Node -> visitNode(child.node, childIndent, lines)
                    }
                }
            }
        }
    }

    private fun toRenderNode(node: NanoDOMTreeNode): RenderNode? {
        if (node.invisible == true) {
            return null
        }
        if (isTextNode(node)) {
            return null
        }

        val children = node.children.orEmpty()
            .mapNotNull { child ->
                if (isTextNode(child)) {
                    normalizeText(child.nodeValue)
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { RenderChild.Text(it) }
                } else {
                    toRenderNode(child)?.let { RenderChild.Node(it) }
                }
            }
            .let { normalizeChildren(it, accessibleName(node)) }

        val role = role(node) ?: return null
        val props = renderProps(node, role)

        if (children.isEmpty() && props.isEmpty() && node.ref <= 0 && accessibleName(node).isNullOrEmpty()) {
            return null
        }

        return RenderNode(
            role = role,
            name = accessibleName(node),
            checked = triState(stringAttributes(node)["checked"] ?: stringAttributes(node)["aria-checked"]),
            disabled = booleanAttribute(stringAttributes(node)["disabled"] ?: stringAttributes(node)["aria-disabled"]),
            expanded = booleanAttribute(stringAttributes(node)["expanded"] ?: stringAttributes(node)["aria-expanded"]),
            level = level(stringAttributes(node)),
            pressed = triState(stringAttributes(node)["pressed"] ?: stringAttributes(node)["aria-pressed"]),
            selected = booleanAttribute(stringAttributes(node)["selected"] ?: stringAttributes(node)["aria-selected"]),
            ref = node.ref.takeIf { it > 0 }?.let { "e$it" },
            cursorPointer = node.interactive == true,
            props = props,
            children = children
        )
    }

    private fun createKey(node: RenderNode): String {
        var key = node.role
        node.name?.let { name ->
            if (name.length <= 900) {
                key += " " + jsonString(name)
            }
        }
        when (node.checked) {
            TriState.MIXED -> key += " [checked=mixed]"
            TriState.TRUE -> key += " [checked]"
            null -> Unit
        }
        if (node.disabled) {
            key += " [disabled]"
        }
        if (node.expanded) {
            key += " [expanded]"
        }
        node.level?.let { key += " [level=$it]" }
        when (node.pressed) {
            TriState.MIXED -> key += " [pressed=mixed]"
            TriState.TRUE -> key += " [pressed]"
            null -> Unit
        }
        if (node.selected) {
            key += " [selected]"
        }
        node.ref?.let { ref ->
            key += " [ref=$ref]"
            if (node.cursorPointer) {
                key += " [cursor=pointer]"
            }
        }
        return key
    }

    private fun renderProps(node: NanoDOMTreeNode, role: String): LinkedHashMap<String, String> {
        val attributes = stringAttributes(node)
        val props = linkedMapOf<String, String>()
        if (role == "link") {
            attributes["href"]?.takeIf { it.isNotBlank() }?.let { props["url"] = it }
        }
        if (role == "textbox") {
            val placeholder = attributes["placeholder"] ?: attributes["aria-placeholder"]
            if (!placeholder.isNullOrBlank() && placeholder != accessibleName(node)) {
                props["placeholder"] = placeholder
            }
        }
        return props
    }

    private fun accessibleName(node: NanoDOMTreeNode): String? {
        val attributes = stringAttributes(node)
        val role = role(node)
        return normalizeText(
            attributes["ax_name"]
                ?: attributes["aria-label"]
                ?: attributes["title"]
                ?: if (role == "img") attributes["alt"] else null
        )
    }

    private fun level(attributes: Map<String, String>): String? {
        val raw = attributes["level"] ?: attributes["aria-level"]
        return raw?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun role(node: NanoDOMTreeNode): String? {
        val role = stringAttributes(node)["role"]?.trim()
        return when {
            role.isNullOrEmpty() -> if (isTextNode(node)) null else "generic"
            role.equals("none", ignoreCase = true) || role.equals("presentation", ignoreCase = true) -> null
            else -> role
        }
    }

    private fun normalizeChildren(children: List<RenderChild>, accessibleName: String?): List<RenderChild> {
        if (children.size == 1 && children.first() is RenderChild.Text) {
            val childText = (children.first() as RenderChild.Text).value
            if (accessibleName != null && childText == accessibleName) {
                return emptyList()
            }
        }
        return children
    }

    private fun isTextNode(node: NanoDOMTreeNode): Boolean {
        val nodeName = node.nodeName?.trim()?.lowercase(Locale.ROOT)
        return nodeName == "#text" || nodeName == "text"
    }

    private fun stringAttributes(node: NanoDOMTreeNode): Map<String, String> {
        return node.attributes.orEmpty().mapValues { (_, value) -> value.toString() }
    }

    private fun normalizeText(value: String?): String? {
        return value
            ?.replace(whitespaceRegex, " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun yamlEscapeKeyIfNeeded(value: String): String {
        return if (requiresYamlQuoting(value, isKey = true)) jsonString(value) else value
    }

    private fun yamlEscapeValueIfNeeded(value: String): String {
        return if (requiresYamlQuoting(value, isKey = false)) jsonString(value) else value
    }

    private fun requiresYamlQuoting(value: String, isKey: Boolean): Boolean {
        if (value.isEmpty()) {
            return true
        }
        if (value.trim() != value || '\n' in value || '\r' in value || '\t' in value) {
            return true
        }
        if (value.lowercase(Locale.ROOT) in yamlKeywords) {
            return true
        }
        if (value.startsWith("#") || value.startsWith("- ") || value.startsWith("!")) {
            return true
        }
        if (value.contains(": ") || value.endsWith(":")) {
            return true
        }
        if (!isKey && (value.startsWith("{") || value.startsWith("[") || value.startsWith("&") || value.startsWith("*"))) {
            return true
        }
        return false
    }

    private fun jsonString(value: String): String {
        return buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
    }

    private fun booleanAttribute(raw: String?): Boolean {
        return when (raw?.trim()?.lowercase(Locale.ROOT)) {
            null, "", "false", "0", "off", "no" -> false
            else -> true
        }
    }

    private fun triState(raw: String?): TriState? {
        return when (raw?.trim()?.lowercase(Locale.ROOT)) {
            null, "", "false", "0", "off", "no" -> null
            "mixed" -> TriState.MIXED
            else -> TriState.TRUE
        }
    }

    private sealed interface RenderChild {
        data class Text(val value: String) : RenderChild
        data class Node(val node: RenderNode) : RenderChild
    }

    private enum class TriState {
        TRUE,
        MIXED
    }

    private data class RenderNode(
        val role: String,
        val name: String?,
        val checked: TriState?,
        val disabled: Boolean,
        val expanded: Boolean,
        val level: String?,
        val pressed: TriState?,
        val selected: Boolean,
        val ref: String?,
        val cursorPointer: Boolean,
        val props: LinkedHashMap<String, String>,
        val children: List<RenderChild>
    )

    private fun RenderNode.singleInlinedTextChild(): String? {
        if (props.isNotEmpty() || children.size != 1) {
            return null
        }
        val child = children.first()
        return if (child is RenderChild.Text) child.value else null
    }
}
