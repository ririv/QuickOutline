package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

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
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Tab) {
                        onValueChange(handleTab(value, it.isShiftPressed))
                        true // Consume the event
                    } else {
                        false // Do not consume
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