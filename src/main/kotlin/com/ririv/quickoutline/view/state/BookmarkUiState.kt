package com.ririv.quickoutline.view.state

import androidx.compose.ui.text.input.TextFieldValue
import com.ririv.quickoutline.model.Bookmark

data class BookmarkUiState(
    val rootBookmark: Bookmark? = null,
    val selectedBookmark: Bookmark? = null,
    val filePath: String = "",
    val offset: TextFieldValue = TextFieldValue(""),
    val textInput: TextFieldValue = TextFieldValue(""),
    val recomposeTrigger: Int = 0
)
