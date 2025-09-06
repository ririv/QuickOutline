package com.ririv.quickoutline.view.ui

import androidx.compose.runtime.Composable
import com.ririv.quickoutline.model.Bookmark
import com.ririv.quickoutline.view.ui.bookmarktab.BookmarkTree

@Composable
fun TreeSubView(
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
