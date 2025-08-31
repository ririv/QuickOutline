package com.ririv.quickoutline.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ririv.quickoutline.model.Bookmark

@Composable
fun BookmarkTree(
    bookmarks: List<Bookmark>,
    selectedBookmark: Bookmark?,
    onBookmarkSelected: (Bookmark) -> Unit
) {
    val flattenedBookmarks = flattenBookmarks(bookmarks)
    LazyColumn {
        items(flattenedBookmarks) { bookmark ->
            val backgroundColor = if (bookmark == selectedBookmark) {
                Color.LightGray
            } else {
                Color.Transparent
            }
            Box(modifier = Modifier.background(backgroundColor)) {
                Text(
                    text = "  ".repeat(bookmark.level) + bookmark.title,
                    modifier = Modifier.clickable { onBookmarkSelected(bookmark) }
                )
            }
        }
    }
}

fun flattenBookmarks(bookmarks: List<Bookmark>): List<Bookmark> {
    val flattenedList = mutableListOf<Bookmark>()
    for (bookmark in bookmarks) {
        flattenedList.add(bookmark)
        if (bookmark.children.isNotEmpty()) {
            flattenedList.addAll(flattenBookmarks(bookmark.children))
        }
    }
    return flattenedList
}
