package com.ririv.quickoutline.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BookmarkBottomPane(viewModel: BookmarkViewModel) {
    var offset by remember { mutableStateOf("") }
    var newTitle by remember { mutableStateOf("") }

    Row(modifier = Modifier.padding(8.dp)) {
        IconButton(onClick = { viewModel.deleteBookmark() }) {
            Text("D")
        }
        Button(onClick = { viewModel.addBookmark(newTitle) }) {
            Text("Add")
        }
        TextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("New Title") },
            modifier = Modifier.weight(1f)
        )
        Button(onClick = { viewModel.editBookmark(newTitle) }) {
            Text("Edit")
        }
        TextField(
            value = offset,
            onValueChange = { offset = it },
            label = { Text("Offset") },
            modifier = Modifier.weight(1f)
        )
    }
}
