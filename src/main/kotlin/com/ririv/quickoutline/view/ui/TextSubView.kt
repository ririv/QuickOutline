package com.ririv.quickoutline.view.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import com.ririv.quickoutline.view.controls.MultilineTextFieldWithTabSupport
import com.ririv.quickoutline.view.controls.StyledButton

@Composable
fun TextSubView(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onAutoFormatClick: () -> Unit,
    onVsCodeClick: () -> Unit,
    isSyncingWithEditor: Boolean
) {
    var selectedMethod by remember { mutableStateOf(Method.SEQ) } // Use the enum

    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            MultilineTextFieldWithTabSupport(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxSize(),
                placeholder = { Text(stringResource("contentsTextArea.prompt")) },
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
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.3f), // 给文字一个更深的背景
                    ) {
                        Text(
                            text = stringResource("mask.text"),
                            color = Color.White, // 文字用纯白色
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
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