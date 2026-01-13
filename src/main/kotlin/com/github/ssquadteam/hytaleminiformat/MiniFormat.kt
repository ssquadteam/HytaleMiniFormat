package com.github.ssquadteam.hytaleminiformat

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.protocol.MaybeBool
import java.awt.Color
import java.util.regex.Pattern

object MiniFormat {

    private val TAG_PATTERN = Pattern.compile("<(/?[^<>]*)>")

    fun parse(input: String): Message {
        if (!input.contains("<")) {
            return Message.raw(input)
        }

        val tokens = tokenize(input)
        return render(tokens)
    }

    private sealed class Token {
        data class Text(val content: String) : Token()
        data class OpenTag(val content: String) : Token()
        data class CloseTag(val content: String) : Token()
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val matcher = TAG_PATTERN.matcher(input)
        var lastIndex = 0

        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                tokens.add(Token.Text(input.substring(lastIndex, matcher.start())))
            }
            
            val content = matcher.group(1)
            if (content.startsWith("/")) {
                tokens.add(Token.CloseTag(content.substring(1)))
            } else {
                tokens.add(Token.OpenTag(content))
            }
            lastIndex = matcher.end()
        }

        if (lastIndex < input.length) {
            tokens.add(Token.Text(input.substring(lastIndex)))
        }
        return tokens
    }

    private data class StyleState(
        var color: String? = null,
        var bold: Boolean = false,
        var italic: Boolean = false,
        var underlined: Boolean = false,
        var monospace: Boolean = false,
        var gradient: GradientSpec? = null
    )
    
    private data class GradientSpec(val colors: List<String>, var index: Int = 0, val totalLength: Int = 0)

    private fun render(tokens: List<Token>): Message {
        val root = Node("root")
        buildTree(tokens, root)
        return root.render()
    }

    private class Node(val tag: String, val parent: Node? = null) {
        val children = mutableListOf<Any>() // String or Node
        
        fun render(parentStyle: StyleState = StyleState()): Message {
            val myStyle = parentStyle.copy()
            applyTagToStyle(tag, myStyle)
            
            var gradientLength = 0
            if (myStyle.gradient != null) {
                gradientLength = calculateTextLength()
            }
            
            val messages = mutableListOf<Message>()
            var currentGradientIndex = 0
            
            for (child in children) {
                if (child is String) {
                    if (myStyle.gradient != null) {
                        for (char in child) {
                            val msg = Message.raw(char.toString())
                            applyStyle(msg, myStyle)
                            
                            val color = interpolateGradient(myStyle.gradient!!.colors, currentGradientIndex, gradientLength)
                            msg.color(color)
                            
                            messages.add(msg)
                            currentGradientIndex++
                        }
                    } else {
                        val msg = Message.raw(child)
                        applyStyle(msg, myStyle)
                        messages.add(msg)
                    }
                } else if (child is Node) {
                    messages.add(child.render(myStyle))
                }
            }
            
            return if (messages.isEmpty()) Message.raw("") else Message.join(*messages.toTypedArray())
        }
        
        private fun calculateTextLength(): Int {
            var len = 0
            for (child in children) {
                if (child is String) len += child.length
                else if (child is Node) len += child.calculateTextLength()
            }
            return len
        }

        private fun applyStyle(msg: Message, style: StyleState) {
            if (style.color != null && style.gradient == null) msg.color(style.color!!)
            if (style.bold) msg.bold(true)
            if (style.italic) msg.italic(true)
            if (style.underlined) {
                 msg.formattedMessage.underlined = MaybeBool.True
            }
            if (style.monospace) msg.monospace(true)
        }
    }
    
    private fun buildTree(tokens: List<Token>, root: Node) {
        var current = root
        
        for (token in tokens) {
            when (token) {
                is Token.Text -> current.children.add(token.content)
                is Token.OpenTag -> {
                     val node = Node(token.content, current)
                     current.children.add(node)
                     current = node
                }
                is Token.CloseTag -> {
                    if (current.parent != null) {
                        current = current.parent!!
                    }
                }
            }
        }
    }

    private fun applyTagToStyle(tag: String, style: StyleState) {
        if (tag == "root") return
        
        val parts = tag.split(":")
        val name = parts[0].lowercase()
        
        when (name) {
            "bold", "b" -> style.bold = true
            "italic", "i", "em" -> style.italic = true
            "underlined", "u" -> style.underlined = true
            "monospace", "mono", "tt" -> style.monospace = true
            "gradient" -> {
                 if (parts.size >= 3) {
                     val colors = parts.drop(1).map { resolveColor(it) ?: "#FFFFFF" }
                     style.gradient = GradientSpec(colors)
                     style.color = null
                 }
            }
            else -> {
                val resolved = resolveColor(tag)
                if (resolved != null) {
                    style.color = resolved
                } else if (name == "color" && parts.size > 1) {
                     style.color = resolveColor(parts[1])
                }
            }
        }
    }

    private fun resolveColor(input: String): String? {
        if (input.startsWith("#") && input.length == 7) return input
        return when(input.lowercase()) {
            "black" -> Colors.BLACK
            "dark_blue" -> Colors.DARK_BLUE
            "dark_green" -> Colors.DARK_GREEN
            "dark_aqua" -> Colors.DARK_AQUA
            "dark_red" -> Colors.DARK_RED
            "dark_purple" -> Colors.DARK_PURPLE
            "gold" -> Colors.GOLD
            "gray" -> Colors.GRAY
            "dark_gray" -> Colors.DARK_GRAY
            "blue" -> Colors.BLUE
            "green" -> Colors.GREEN
            "aqua" -> Colors.AQUA
            "red" -> Colors.RED
            "light_purple" -> Colors.LIGHT_PURPLE
            "yellow" -> Colors.YELLOW
            "white" -> Colors.WHITE
            else -> null
        }
    }

    private fun interpolateGradient(colors: List<String>, index: Int, total: Int): String {
        if (colors.isEmpty()) return "#FFFFFF"
        if (colors.size == 1) return colors[0]
        if (total <= 1) return colors[0]

        val segmentSize = total.toFloat() / (colors.size - 1)
        val segment = (index / segmentSize).toInt().coerceIn(0, colors.size - 2)
        val fraction = (index % segmentSize) / segmentSize

        val c1 = Color.decode(colors[segment])
        val c2 = Color.decode(colors[segment + 1])

        val r = (c1.red + (c2.red - c1.red) * fraction).toInt()
        val g = (c1.green + (c2.green - c1.green) * fraction).toInt()
        val b = (c1.blue + (c2.blue - c1.blue) * fraction).toInt()

        return String.format("#%02x%02x%02x", r, g, b)
    }
}
