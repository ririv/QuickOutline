package com.ririv.quickoutline.view.ui.bookmarktab

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.model.Bookmark
import com.ririv.quickoutline.view.ui.stringResource

private fun getAllBookmarks(bookmarks: List<Bookmark>): List<Bookmark> {
    val allBookmarks = mutableListOf<Bookmark>()
    for (bookmark in bookmarks) {
        allBookmarks.add(bookmark)
        if (bookmark.children.isNotEmpty()) {
            allBookmarks.addAll(getAllBookmarks(bookmark.children))
        }
    }
    return allBookmarks
}

@Composable
fun BookmarkTree(
    bookmarks: List<Bookmark>,
    selectedBookmark: Bookmark?,
    onBookmarkSelected: (Bookmark) -> Unit
) {
    val allBookmarks = remember(bookmarks) { getAllBookmarks(bookmarks) }
    var expandAll by remember { mutableStateOf(true) }
    val expansionState = remember { mutableStateMapOf<Bookmark, Boolean>() }

    LaunchedEffect(bookmarks) {
        expansionState.clear()
        allBookmarks.forEach { expansionState[it] = true }
        expandAll = true
    }

    val flattenedBookmarks = remember(bookmarks, expansionState.toMap()) {
        flattenBookmarks(bookmarks, expansionState)
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = 1.dp, color = Color(0xFFD1D1D1))
                .padding(horizontal = 4.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            Icon(
                imageVector = if (expandAll) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                tint = if (isHovered) Color.Black else Color.Gray,
                contentDescription = if (expandAll) "Collapse All" else "Expand All",
                modifier = Modifier.size(24.dp).clickable(
                    interactionSource = interactionSource,
                    indication = null, // No ripple effect
                    onClick = { expandAll = !expandAll
                        if (expandAll) {
                            allBookmarks.forEach { expansionState[it] = true }
                        } else {
                            expansionState.clear()
                        }}
                ),

            )

            Spacer(modifier = Modifier.weight(1f))
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
                        .height(32.dp) // Set a fixed height for the row
                        .background(backgroundColor)
                        .clickable { onBookmarkSelected(bookmark) }
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically // Center content vertically
                ) {
                    Spacer(modifier = Modifier.width(((bookmark.level - 1).coerceAtLeast(0) * 16).dp))

                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()

                    if (bookmark.children.isNotEmpty()) {
                        Icon(
                            imageVector = if (expansionState[bookmark] == true) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                            contentDescription = "Expand/Collapse",
                            tint = if (isHovered) Color.Black else Color.Gray,
                            modifier = Modifier.size(24.dp).clickable(
                                interactionSource = interactionSource,
                                indication = null, // No ripple effect
                                onClick = { expansionState[bookmark] = !(expansionState[bookmark] ?: false) }
                            )
                        )
                    } else {
                        Spacer(modifier = Modifier.width(24.dp)) // To align the text
                    }
                    Text(
                        text = bookmark.title,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        color = Color.Black
                    )
                    Text(
                        text = bookmark.pageNum.map { it.toString() }.orElse(""),
                        modifier = Modifier.width(100.dp),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

fun flattenBookmarks(
    bookmarks: List<Bookmark>,
    expansionState: Map<Bookmark, Boolean>
): List<Bookmark> {
    val flattenedList = mutableListOf<Bookmark>()
    for (bookmark in bookmarks) {
        flattenedList.add(bookmark)
        if (expansionState[bookmark] == true && bookmark.children.isNotEmpty()) {
            flattenedList.addAll(flattenBookmarks(bookmark.children, expansionState))
        }
    }
    return flattenedList
}
