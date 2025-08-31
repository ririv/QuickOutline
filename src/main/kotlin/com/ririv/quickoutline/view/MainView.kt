package com.ririv.quickoutline.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainView() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Bookmark", "TOC Generator", "Page Label", "PDF Preview")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Outline") },
                actions = {
                    var filePath by remember { mutableStateOf("") }
                    TextField(
                        value = filePath,
                        onValueChange = { filePath = it },
                        label = { Text("File Path") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { /* TODO: Implement file chooser */ }) {
                        Text("O")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Text("Message Container")
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(it)) {
            LeftPane(selectedTab) { newTab -> selectedTab = newTab }
            Column {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(text = title) })
                    }
                }
                when (selectedTab) {
                    0 -> BookmarkTab()
                    1 -> TocGeneratorTab()
                    2 -> PageLabelTabView()
                    3 -> PdfPreviewTabView()
                }
            }
        }
    }
}
