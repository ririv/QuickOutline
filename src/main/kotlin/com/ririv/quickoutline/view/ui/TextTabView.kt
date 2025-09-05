package com.ririv.quickoutline.view.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.textProcess.methods.Method
import com.ririv.quickoutline.view.controls.ButtonType
import com.ririv.quickoutline.view.controls.StyledButton
import com.ririv.quickoutline.view.controls.StyledTextField
import com.ririv.quickoutline.view.ui.stringResource

@Composable
fun TextTabView(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onAutoFormatClick: () -> Unit,
    onVsCodeClick: () -> Unit,
    isSyncingWithEditor: Boolean
) {
    var selectedMethod by remember { mutableStateOf(Method.SEQ) } // Use the enum

    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            StyledTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Tab) {
                            onValueChange(handleTab(value, event.isShiftPressed))
                            true // Consume the event
                        } else {
                            false // Do not consume
                        }
                    },
                placeholder = { Text(stringResource("contentsTextArea.prompt")) },
                singleLine = false,
                enabled = !isSyncingWithEditor
            )
            if (isSyncingWithEditor) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.1f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource("mask.text"), color = Color.White)
                }
            }
        }
        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFFDFDFDF)))
        Column(
            modifier = Modifier.width(135.dp).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StyledButton(

                    onClick = onVsCodeClick,
                    text = if (isSyncingWithEditor) stringResource("btn.externalEditorConnected") else "VSCode",
                    type = ButtonType.PLAIN_PRIMARY,
                    enabled = !isSyncingWithEditor
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StyledButton(
                    onClick = onAutoFormatClick,
                    text = stringResource("autoFormatBtn.text"),
                    type = ButtonType.PLAIN_PRIMARY
                )
            }
            Column(modifier = Modifier.weight(1f).align(Alignment.CenterHorizontally)) {
                Text(stringResource("Tip.text1"))
                Text(stringResource("Tip.text2"))
                Text(stringResource("Tip.text3"))
            }

            val radioOptions = listOf(
                Method.SEQ,
                Method.INDENT
            )
            Column {
                radioOptions.forEach { method ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()

                    val textColor = if (isHovered) {
                        if (selectedMethod == method) Color(51, 126, 204) else Color(0xFF409EFF)
                    } else {
                        Color(0xFF606266)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { selectedMethod = method }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedMethod == method),
                            onClick = { selectedMethod = method },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = if (isHovered) Color(51, 126, 204) else Color(0xFF409EFF),
                                unselectedColor = if (isHovered) Color(0xFF409EFF) else Color.Gray
                            )
                        )
                        Text(
                            text = when (method) {
                                Method.SEQ -> stringResource("bookmarkTab.seqRBtn.text")
                                Method.INDENT -> stringResource("bookmarkTab.indentRBtn.text")
                            },
                            color = textColor,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
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

    val newLines = lines.mapIndexed { index, line ->
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