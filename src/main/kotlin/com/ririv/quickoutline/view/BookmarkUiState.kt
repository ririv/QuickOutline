package com.ririv.quickoutline.view

import com.ririv.quickoutline.model.Bookmark

data class BookmarkUiState(
    val rootBookmark: Bookmark? = null,
    val selectedBookmark: Bookmark? = null,
    val filePath: String = "",
    val offset: Int? = null,
    val recomposeTrigger: Int = 0
)
