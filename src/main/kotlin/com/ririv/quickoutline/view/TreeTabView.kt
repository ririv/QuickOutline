package com.ririv.quickoutline.view

import androidx.compose.runtime.Composable
import com.ririv.quickoutline.model.Bookmark

@Composable
fun TreeTabView(
    bookmarks: List<Bookmark>,
    selectedBookmark: Bookmark?,
    onBookmarkSelected: (Bookmark) -> Unit
) {
    BookmarkTree(
        bookmarks = bookmarks,
        selectedBookmark = selectedBookmark,
        onBookmarkSelected = onBookmarkSelected
    )
}
