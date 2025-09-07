package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MultilineTextFieldWithTabSupport(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor = when {
        isFocused -> Color(0xFF409EFF)
        isHovered -> Color(0xFF409EFF)
        else -> Color.Transparent
    }

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .hoverable(
                interactionSource = interactionSource
            )
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Tab) {
                    val newValue = if (keyEvent.isShiftPressed) {
                        removeIndent(value)
                    } else {
                        addIndent(value)
                    }
                    onValueChange(newValue)
                    true
                } else {
                    false
                }
            },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        interactionSource = interactionSource,
        placeholder = placeholder,
        singleLine = false,
        enabled = enabled
    )
}

private val INDENT_PATTERN: Pattern = Pattern.compile("^(\\t|\\s{1,4})")

private fun addIndent(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val selection = value.selection
    val tab = "\t"

    if (selection.collapsed) {
        val newText = text.replaceRange(selection.start, selection.end, tab)
        return TextFieldValue(newText, TextRange(selection.start + 1))
    }

    val start = min(selection.start, selection.end)
    val end = max(selection.start, selection.end)

    val isMultiLine = text.substring(start, end).contains('\n')

    if (isMultiLine) {
        // Multi-line indent logic
        val startLine = text.substring(0, start).count { it == '\n' }
        val endLine = text.substring(0, end).count { it == '\n' }

        val lines = text.split('\n').toMutableList()
        var charsAdded = 0
        for (i in startLine..endLine) {
            if (i < lines.size && lines[i].isNotEmpty()) {
                lines[i] = tab + lines[i]
                charsAdded++
            }
        }

        val newText = lines.joinToString("\n")
        return TextFieldValue(newText, TextRange(start, end + charsAdded))
    } else {
        // Single-line selection logic
        val lineStart = text.lastIndexOf('\n', start - 1) + 1
        val lineEnd = text.indexOf('\n', start).let { if (it == -1) text.length else it }

        val isFullLineSelected = start == lineStart && (end == lineEnd || (end == lineEnd + 1 && text.getOrNull(lineEnd) == '\n'))

        if (isFullLineSelected) {
            // Indent the line by inserting a tab at the beginning.
            val newText = text.replaceRange(lineStart, lineStart, tab)
            return TextFieldValue(newText, TextRange(start, end + 1))
        } else {
            // Replace partial selection with a tab.
            val newText = text.replaceRange(start, end, tab)
            return TextFieldValue(newText, TextRange(start + 1))
        }
    }
}

private fun removeIndent(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val selection = value.selection

    var start = min(selection.start, selection.end)
    val end = max(selection.start, selection.end)

    // If the selection starts right after a newline, we should not affect the previous line.
    if (start > 0 && start < text.length && text[start - 1] == '\n') {
        // This case is handled by the line-based logic below
    } else if (selection.collapsed) {
        val caretPosition = selection.start
        if (caretPosition == 0) return value

        val lineStart = text.lastIndexOf('\n', caretPosition - 1) + 1
        val line = text.substring(lineStart)

        val matcher = INDENT_PATTERN.matcher(line)
        if (matcher.find()) {
            val indent = matcher.group(0) ?: ""
            val len = indent.length
            if (lineStart + len <= text.length) {
                val newText = text.removeRange(lineStart, lineStart + len)
                return TextFieldValue(newText, TextRange((caretPosition - len).coerceAtLeast(lineStart)))
            }
        }
        return value // No indent to remove
    }

    // Multi-line or selection-based un-indent
    if (start < text.length && text[start] == '\n') {
        start++
    }
    if (start >= end) return value

    val startLineNum = text.substring(0, start).count { it == '\n' }
    val effectiveEnd = if (end > 0 && text[end - 1] == '\n') end - 1 else end
    val endLineNum = text.substring(0, effectiveEnd).count { it == '\n' }

    val lines = text.split('\n').toMutableList()
    var charsRemoved = 0
    var firstLineCharsRemoved = 0

    for (i in startLineNum..endLineNum) {
        if (i < lines.size) {
            val matcher = INDENT_PATTERN.matcher(lines[i])
            if (matcher.find()) {
                val indent = matcher.group(0) ?: ""
                val len = indent.length
                lines[i] = lines[i].substring(len)
                charsRemoved += len
                if (i == startLineNum) {
                    firstLineCharsRemoved = len
                }
            }
        }
    }

    val newText = lines.joinToString("\n")

    val newStart = (start - firstLineCharsRemoved).coerceAtLeast(0)
    val newEnd = (end - charsRemoved).coerceAtLeast(0)

    return TextFieldValue(
        newText,
        TextRange(newStart, newEnd)
    )
}