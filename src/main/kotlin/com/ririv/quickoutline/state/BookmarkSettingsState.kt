package com.ririv.quickoutline.state

import com.ririv.quickoutline.model.Bookmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BookmarkSettingsState {
    private val _offset = MutableStateFlow<Int?>(null)
    val offset = _offset.asStateFlow()

    private val _rootBookmark = MutableStateFlow<Bookmark?>(null)
    val rootBookmark = _rootBookmark.asStateFlow()

    fun setOffset(offset: Int?) {
        _offset.value = offset
    }

    fun setRootBookmark(rootBookmark: Bookmark?) {
        _rootBookmark.value = rootBookmark
    }
}
