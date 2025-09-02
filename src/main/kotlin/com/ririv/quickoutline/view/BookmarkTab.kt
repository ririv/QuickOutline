package com.ririv.quickoutline.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.java.KoinJavaComponent.inject

@Composable
fun BookmarkTab() {
    val viewModel: BookmarkViewModel by inject(BookmarkViewModel::class.java)
    var showTreeView by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    // This will trigger recomposition when the trigger changes
    val recomposeTrigger = uiState.recomposeTrigger

    Column {
        Box(modifier = Modifier.weight(1f)) {
            if (showTreeView) {
                TreeTabView(
                    bookmarks = uiState.rootBookmark?.children ?: emptyList(),
                    selectedBookmark = uiState.selectedBookmark,
                    onBookmarkSelected = { viewModel.selectBookmark(it) }
                )
            } else {
                TextTabView(
                    value = uiState.textInput,
                    onValueChange = { viewModel.onTextInputChange(it) }
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDFDFDF)))
        BookmarkBottomPane(viewModel, showTreeView) { showTreeView = !showTreeView }
    }
}
