package com.ririv.quickoutline.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ririv.quickoutline.model.Bookmark

@Composable
fun TextTabView(bookmarks: List<Bookmark>, onTextChange: (String) -> Unit) {
    val text = bookmarks.joinToString("\n") { "  ".repeat(it.level) + it.title }
    TextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxSize()
    )
}

