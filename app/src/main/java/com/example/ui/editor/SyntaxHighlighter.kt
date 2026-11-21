package com.example.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.model.FileLanguage
import com.example.ui.theme.SyntaxAttribute
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxTag

class SyntaxVisualTransformation(
    private val language: FileLanguage
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = highlightSyntax(text.text, language),
            offsetMapping = OffsetMapping.Identity
        )
    }
}

private val kotlinKeywords = Regex(
    """\b(package|import|class|interface|object|fun|val|var|override|private|public|internal|return|if|else|when|for|while|true|false|null|by|remember|mutableStateOf|mutableIntStateOf|@Composable|data|sealed|enum)\b"""
)
private val jsKeywords = Regex(
    """\b(const|let|var|function|return|if|else|for|while|switch|case|break|true|false|null|undefined|async|await|import|export|default|document|window|console|new)\b"""
)
private val cssPropertyRegex = Regex("""([a-zA-Z-]+)\s*:""")
private val tagRegex = Regex("""</?[a-zA-Z0-9:-]+|>|/>""")
private val attrRegex = Regex("""\b([a-zA-Z:-]+)(?==)""")
private val jsonKeyRegex = Regex(""""([^"\\]*)"\s*:""")
private val stringRegex = Regex(""""([^"\\]|\\.)*"|'([^'\\]|\\.)*'|`([^`\\]|\\.)*`""")
private val numberRegex = Regex("""\b\d+(?:\.\d+)?(?:px|dp|sp|rem|em|vh|vw|%|f|L)?\b""")
private val slashCommentRegex = Regex("""//[^\n]*|/\*[\s\S]*?\*/""")
private val htmlCommentRegex = Regex("""<!--[\s\S]*?-->""")

fun highlightSyntax(code: String, language: FileLanguage): AnnotatedString {
    if (code.length > 24_000) {
        return AnnotatedString(code)
    }
    return buildAnnotatedString {
        append(code)

        fun applyColor(regex: Regex, color: Color) {
            regex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
            }
        }

        when (language) {
            FileLanguage.HTML, FileLanguage.XML -> {
                applyColor(attrRegex, SyntaxAttribute)
                applyColor(tagRegex, SyntaxTag)
                applyColor(stringRegex, SyntaxString)
                applyColor(htmlCommentRegex, SyntaxComment)
            }
            FileLanguage.CSS -> {
                applyColor(cssPropertyRegex, SyntaxFunction)
                applyColor(numberRegex, SyntaxNumber)
                applyColor(stringRegex, SyntaxString)
                applyColor(slashCommentRegex, SyntaxComment)
            }
            FileLanguage.JS -> {
                applyColor(numberRegex, SyntaxNumber)
                applyColor(jsKeywords, SyntaxKeyword)
                applyColor(stringRegex, SyntaxString)
                applyColor(slashCommentRegex, SyntaxComment)
            }
            FileLanguage.KOTLIN, FileLanguage.GRADLE -> {
                applyColor(numberRegex, SyntaxNumber)
                applyColor(kotlinKeywords, SyntaxKeyword)
                applyColor(stringRegex, SyntaxString)
                applyColor(slashCommentRegex, SyntaxComment)
            }
            FileLanguage.JSON -> {
                applyColor(numberRegex, SyntaxNumber)
                applyColor(stringRegex, SyntaxString)
                jsonKeyRegex.findAll(code).forEach { match ->
                    val keyGroup = match.groups[1]
                    if (keyGroup != null) {
                        addStyle(SpanStyle(color = SyntaxTag), keyGroup.range.first - 1, keyGroup.range.last + 2)
                    }
                }
            }
            FileLanguage.TEXT -> Unit
        }
    }
}
