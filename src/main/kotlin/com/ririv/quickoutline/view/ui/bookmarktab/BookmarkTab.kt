package com.ririv.quickoutline.view.ui.bookmarktab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.ui.TextSubView
import com.ririv.quickoutline.view.ui.TreeSubView
import com.ririv.quickoutline.view.viewmodel.BookmarkViewModel
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
                TreeSubView(
                    bookmarks = uiState.rootBookmark?.children ?: emptyList(),
                    selectedBookmark = uiState.selectedBookmark,
                    onBookmarkSelected = { viewModel.selectBookmark(it) }
                )
            } else {
                TextSubView(
                    value = uiState.textInput,
                    onValueChange = { viewModel.onTextInputChange(it) },
                    onAutoFormatClick = { viewModel.autoFormat() },
                    onVsCodeClick = { viewModel.syncWithExternalEditor() },
                    isSyncingWithEditor = uiState.isSyncingWithEditor
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDFDFDF)))
        BookmarkBottomPane(viewModel, showTreeView) { showTreeView = !showTreeView }
    }
}
