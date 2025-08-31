package com.ririv.quickoutline.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.controls.ButtonType
import com.ririv.quickoutline.view.controls.StyledButton
import com.ririv.quickoutline.view.controls.StyledTextField

@Composable
fun BookmarkBottomPane(viewModel: BookmarkViewModel, showTreeView: Boolean, onSwitchView: () -> Unit) {
    var offset by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { viewModel.deleteBookmark() }) {
            Icon(
                painter = painterResource("drawable/delete.svg"),
                contentDescription = "Delete",
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )
        }
        StyledButton(onClick = { /* TODO: Implement Get Contents */ }, text = stringResource("bookmarkTab.getContentsBtn.text"), type = ButtonType.PLAIN_PRIMARY, modifier = Modifier.weight(1f))
        StyledButton(onClick = { /* TODO: Implement Set Contents */ }, text = stringResource("bookmarkTab.setContentsBtn.text"), type = ButtonType.PLAIN_IMPORTANT, modifier = Modifier.weight(1f))
        StyledTextField(value = offset, onValueChange = { offset = it }, placeholder = { Text(stringResource("bookmarkTab.offsetTF.prompt")) }, modifier = Modifier.weight(1f))
        IconButton(onClick = onSwitchView) {
            Icon(
                painter = painterResource(if (showTreeView) "drawable/text-edit.svg" else "drawable/tree-diagram.svg"),
                contentDescription = "Switch view",
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )
        }
    }
}