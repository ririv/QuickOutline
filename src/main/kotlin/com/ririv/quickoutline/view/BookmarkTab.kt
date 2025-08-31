package com.ririv.quickoutline.view

import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.*
import org.koin.java.KoinJavaComponent.inject

@Composable
fun BookmarkTab() {
    val viewModel: BookmarkViewModel by inject(BookmarkViewModel::class.java)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Tree View", "Text View")

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = title) })
            }
        }
        when (selectedTab) {
            0 -> TreeTabView(viewModel.bookmarks, viewModel.selectedBookmark, onBookmarkSelected = { viewModel.selectedBookmark = it })
            1 -> TextTabView(viewModel.bookmarks) { viewModel.updateBookmarksFromText(it) }
        }
        BookmarkBottomPane(viewModel)
    }
}
