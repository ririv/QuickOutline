package com.ririv.quickoutline.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.model.Bookmark
import com.ririv.quickoutline.textProcess.methods.Method
import com.ririv.quickoutline.view.controls.ButtonType
import com.ririv.quickoutline.view.controls.StyledButton
import com.ririv.quickoutline.view.controls.StyledTextField

private object NoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = Color.Unspecified

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f, 0.0f, 0.0f, 0.0f)
}

@Composable
fun TextTabView(bookmarks: List<Bookmark>, onTextChange: (String) -> Unit) {
    val text = bookmarks.joinToString("\n") { "  ".repeat(it.level) + it.title }
    var selectedMethod by remember { mutableStateOf(Method.SEQ) } // Use the enum

    Row(modifier = Modifier.fillMaxSize()) {
        StyledTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            placeholder = { Text(stringResource("contentsTextArea.prompt")) },
            singleLine = false
        )
        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFFDFDFDF)))
        Column(
            modifier = Modifier.width(135.dp).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StyledButton(onClick = { /* TODO: Implement VSCode button */ }, text = "VSCode", type = ButtonType.PLAIN_PRIMARY)
            StyledButton(onClick = { /* TODO: Implement Auto Format button */ }, text = stringResource("autoFormatBtn.text"), type = ButtonType.PLAIN_PRIMARY)
            Text(stringResource("Tip.text1"))
            Text(stringResource("Tip.text2"))
            Text(stringResource("Tip.text3"))

            val radioOptions = listOf(
                stringResource("bookmarkTab.seqRBtn.text") to Method.SEQ,
                stringResource("bookmarkTab.indentRBtn.text") to Method.INDENT
            )
            CompositionLocalProvider(LocalRippleTheme provides NoRippleTheme) {
                Column {
                    radioOptions.forEach { (optionText, method) ->
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
                                text = optionText,
                                color = textColor,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}