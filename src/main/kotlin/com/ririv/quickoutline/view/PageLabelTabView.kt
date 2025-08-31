package com.ririv.quickoutline.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.pdfProcess.PageLabel
import com.ririv.quickoutline.view.controls.ButtonType
import com.ririv.quickoutline.view.controls.StyledButton
import com.ririv.quickoutline.view.controls.StyledTextField
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalMaterialApi::class)
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
                    Text(stringResource("pageLabel.style"), modifier = Modifier.width(80.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        StyledTextField(
                            value = viewModel.numberingStyle.name,
                            onValueChange = {},
                            placeholder = null
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            PageLabel.PageLabelNumberingStyle.values().forEach { style ->
                                DropdownMenuItem(onClick = {
                                    viewModel.numberingStyle = style
                                    expanded = false
                                }) {
                                    Text(style.name)
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource("pageLabel.prefix"), modifier = Modifier.width(80.dp))
                    StyledTextField(
                        value = viewModel.prefix,
                        onValueChange = { viewModel.prefix = it },
                        placeholder = null
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource("pageLabel.startNumber"), modifier = Modifier.width(80.dp))
                    StyledTextField(
                        value = viewModel.startNumber,
                        onValueChange = { viewModel.startNumber = it },
                        placeholder = { Text(stringResource("pageLabel.startNumber.prompt")) }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource("pageLabel.startPage"), modifier = Modifier.width(80.dp))
                    StyledTextField(
                        value = viewModel.fromPage,
                        onValueChange = { viewModel.fromPage = it },
                        placeholder = null
                    )
                }
            }

            StyledButton(onClick = { viewModel.addRule() }, text = stringResource("pageLabel.addRuleBtn"), type = ButtonType.PLAIN_PRIMARY)

            Divider()

            Text(stringResource("pageLabel.ruleList"), fontWeight = FontWeight.Bold, color = Color(0xFF9198A1))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.rules) { rule ->
                    Text("Page ${rule.pageNum}: Style=${rule.numberingStyle}, Prefix='${rule.labelPrefix}', Start=${rule.firstPage}", modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            StyledButton(onClick = { viewModel.setPageLabels() }, text = stringResource("setPageLabelBtn.text"), type = ButtonType.PLAIN_IMPORTANT)
        }
        Column(modifier = Modifier.weight(0.25f)) {
            ThumbnailPane()
        }
    }
}