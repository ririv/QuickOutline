package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun MultilineTextFieldWithTabSupport(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    enabled: Boolean = true
) {
    Box(modifier = modifier) {
        StyledTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Tab) {
                        onValueChange(handleTab(value, it.isShiftPressed))
                        true // Consume the event
                    } else {
                        false // Do not consume
                    }
                },
            placeholder = placeholder,
            singleLine = singleLine,
            enabled = enabled
        )
    }
}

private fun handleTab(value: TextFieldValue, isShiftPressed: Boolean): TextFieldValue {
    val selection = value.selection
    if (selection.collapsed) {
        // No text selected, just insert a tab
        if (isShiftPressed) return value // Or handle un-indenting a single line
        val newText = value.text.substring(0, selection.start) + "\t" + value.text.substring(selection.start)
        return value.copy(text = newText, selection = androidx.compose.ui.text.TextRange(selection.start + 1))
    }

    val lines = value.text.split('\n')
    val selectedLinesRange = getSelectedLines(value.text, selection.start, selection.end)

    val newLines = lines.mapIndexed {
        index, line ->
        if (index >= selectedLinesRange.first && index <= selectedLinesRange.last) {
            if (isShiftPressed) {
                line.removePrefix("\t").removePrefix("  ")
            } else {
                "\t" + line
            }
        } else {
            line
        }
    }

    return value.copy(text = newLines.joinToString("\n"))
}

private fun getSelectedLines(text: String, start: Int, end: Int): IntRange {
    val startLine = text.substring(0, start).count { it == '\n' }
    val endLine = text.substring(0, end).count { it == '\n' }
    return startLine..endLine
}