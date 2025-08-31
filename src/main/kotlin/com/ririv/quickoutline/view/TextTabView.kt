package com.ririv.quickoutline.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.model.Bookmark
import com.ririv.quickoutline.view.controls.ButtonType
import com.ririv.quickoutline.view.controls.RadioButton2Group
import com.ririv.quickoutline.view.controls.StyledButton
import com.ririv.quickoutline.view.controls.StyledTextField

@Composable
fun TextTabView(bookmarks: List<Bookmark>, onTextChange: (String) -> Unit) {
    val text = bookmarks.joinToString("\n") { "  ".repeat(it.level) + it.title }
    var selectedMethod by remember { mutableStateOf("seq") }

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
            RadioButton2Group(
                items = listOf(stringResource("bookmarkTab.seqRBtn.text"), stringResource("bookmarkTab.indentRBtn.text")),
                selectedItem = selectedMethod,
                onItemSelected = { selectedMethod = it }
            )
        }
    }
}

