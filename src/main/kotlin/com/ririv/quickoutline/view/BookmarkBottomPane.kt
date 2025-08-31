package com.ririv.quickoutline.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.controls.ButtonType
import com.ririv.quickoutline.view.controls.StyledButton
import com.ririv.quickoutline.view.controls.StyledTextField

@Composable
fun BookmarkBottomPane(viewModel: BookmarkViewModel, onSwitchView: () -> Unit) {
    var offset by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledButton(onClick = { viewModel.deleteBookmark() }, text = "D", type = ButtonType.PLAIN_IMPORTANT)
        StyledButton(onClick = { /* TODO: Implement Get Contents */ }, text = "Get Contents", type = ButtonType.PLAIN_PRIMARY, modifier = Modifier.weight(1f))
        StyledButton(onClick = { /* TODO: Implement Set Contents */ }, text = "Set Contents", type = ButtonType.PLAIN_IMPORTANT, modifier = Modifier.weight(1f))
        StyledTextField(value = offset, onValueChange = { offset = it }, placeholder = { Text("Offset") }, modifier = Modifier.weight(1f))
        StyledButton(onClick = onSwitchView, text = "Switch View", modifier = Modifier.weight(1f))
    }
}
