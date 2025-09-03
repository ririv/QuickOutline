package com.ririv.quickoutline.view.state

import androidx.compose.ui.text.input.TextFieldValue
import com.ririv.quickoutline.model.Bookmark

data class BookmarkUiState(
    val rootBookmark: Bookmark? = null,
    val selectedBookmark: Bookmark? = null,
    val textInput: TextFieldValue = TextFieldValue(),
    val offset: TextFieldValue = TextFieldValue(),
    val filePath: String = "",
    val isSyncingWithEditor: Boolean = false, // Add this state
    val recomposeTrigger: Int = 0
)
