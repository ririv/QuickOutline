package com.ririv.quickoutline.view.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.model.Bookmark

@Composable
fun BookmarkTree(
    bookmarks: List<Bookmark>,
    selectedBookmark: Bookmark?,
    onBookmarkSelected: (Bookmark) -> Unit
) {
    val flattenedBookmarks = flattenBookmarks(bookmarks)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = 1.dp, color = Color(0xFFD1D1D1))
                .padding(8.dp)
        ) {
            Text(stringResource("titleColumn.text"), modifier = Modifier.weight(1f), color = Color(0xFF888888))
            Text(stringResource("offsetPageColumn.text"), modifier = Modifier.width(100.dp), color = Color(0xFF888888))
        }
        LazyColumn {
            items(flattenedBookmarks) { bookmark ->
                val backgroundColor = if (bookmark == selectedBookmark) {
                    Color(0xFFF4F4F4) // Use a light gray for selection to avoid ambiguity with hover
                } else {
                    Color.Transparent
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .clickable { onBookmarkSelected(bookmark) }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "  ".repeat(bookmark.level) + bookmark.title,
                        modifier = Modifier.weight(1f),
                        color = Color.Black
                    )
                    Text(
                        text = bookmark.offsetPageNum.map { it.toString() }.orElse(""),
                        modifier = Modifier.width(100.dp),
                        color = Color.Black
                    )
                }
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
