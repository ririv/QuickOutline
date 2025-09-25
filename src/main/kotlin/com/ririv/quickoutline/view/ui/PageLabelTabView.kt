package com.ririv.quickoutline.view.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.pdfProcess.PageLabel
import com.ririv.quickoutline.view.controls.ButtonType
import com.ririv.quickoutline.view.controls.StyledButton
import com.ririv.quickoutline.view.controls.StyledTextField
import com.ririv.quickoutline.view.icons.AppIcon
import com.ririv.quickoutline.view.theme.QuickOutlineTheme
import com.ririv.quickoutline.view.viewmodel.PageLabelViewModel
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PageLabelTabView() {
    val viewModel: PageLabelViewModel by inject(PageLabelViewModel::class.java)
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(0.75f).padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource("pageLabel.style"),
                        modifier = Modifier.width(80.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right
                    )
                    val styleDisplayNames = mapOf(
                        PageLabel.PageLabelNumberingStyle.NONE to "无",
                        PageLabel.PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS to "1, 2, 3, ...",
                        PageLabel.PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS to "i, ii, iii, ...",
                        PageLabel.PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS to "I, II, III, ...",
                        PageLabel.PageLabelNumberingStyle.LOWERCASE_LETTERS to "a, b, c, ...",
                        PageLabel.PageLabelNumberingStyle.UPPERCASE_LETTERS to "A, B, C, ...",
                    )
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        StyledTextField(
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            value = TextFieldValue(styleDisplayNames[viewModel.numberingStyle] ?: ""),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            QuickOutlineTheme {
                                PageLabel.PageLabelNumberingStyle.entries.forEach { style ->
                                    DropdownMenuItem(
                                        text = { Text(styleDisplayNames[style] ?: "") },
                                        onClick = {
                                            viewModel.numberingStyle = style
                                            expanded = false
                                        })
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource("pageLabel.prefix"),
                        modifier = Modifier.width(80.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right
                    )
                    StyledTextField(
                        value = viewModel.prefix,
                        onValueChange = { viewModel.prefix = it },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource("pageLabel.startNumber"),
                        modifier = Modifier.width(80.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right
                    )
                    StyledTextField(
                        value = viewModel.startNumber,
                        onValueChange = { viewModel.startNumber = it },
                        placeholder = { Text(stringResource("pageLabel.startNumber.prompt")) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource("pageLabel.startPage"),
                        modifier = Modifier.width(80.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right
                    )
                    StyledTextField(
                        value = viewModel.fromPage,
                        onValueChange = { viewModel.fromPage = it },
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                StyledButton(
                    onClick = { viewModel.addRule() },
                    text = stringResource("pageLabel.addRuleBtn"),
                    type = ButtonType.PLAIN_PRIMARY
                )
            }

            HorizontalDivider()

            Text(stringResource("pageLabel.ruleList"), fontWeight = FontWeight.Bold, color = Color(0xFF9198A1))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val state = rememberLazyListState()
                LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                    items(viewModel.rules) { rule ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Page ${rule.pageNum}: Style=${rule.numberingStyle}, Prefix='${rule.labelPrefix}', Start=${rule.firstPage}",
                                modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                            )
                            val interactionSource = remember { MutableInteractionSource() }
                            val isHovered by interactionSource.collectIsHoveredAsState()
                            val isPressed by interactionSource.collectIsPressedAsState()

                            val targetIconColor = when {
                                isPressed -> Color(0xFFC45656) // Pressed red
                                isHovered -> Color(0xFFF56C6C) // Hover red
                                else -> Color.Gray
                            }
                            val iconColor by animateColorAsState(targetIconColor)
                            Box(
                                modifier = Modifier.clickable(
                                    onClick = { viewModel.removeRule(rule) },
                                    interactionSource = interactionSource,
                                    indication = null
                                ).padding(8.dp)
                            ) {
                                com.ririv.quickoutline.view.icons.AppIcon(icon = AppIcon.DeleteCn, modifier = Modifier.size(20.dp), tint = iconColor)
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState = state)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                StyledButton(
                    onClick = { viewModel.setPageLabels() },
                    text = stringResource("setPageLabelBtn.text"),
                    type = ButtonType.PLAIN_IMPORTANT
                )
            }
        }
        Column(modifier = Modifier.weight(0.25f)) {
            ThumbnailPane()
        }
    }
}