package com.ririv.quickoutline.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.controls.MessageContainer
import com.ririv.quickoutline.view.controls.rememberMessageContainerState
import com.ririv.quickoutline.view.controls.StyledTextField
import org.koin.java.KoinJavaComponent.inject

@Composable
fun MainView() {
    val bookmarkViewModel: BookmarkViewModel by inject(BookmarkViewModel::class.java)
    val messageContainerState = rememberMessageContainerState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Pane
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF2F2F2))) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StyledTextField(
                    value = bookmarkViewModel.filePath,
                    onValueChange = { bookmarkViewModel.filePath = it },
                    placeholder = { Text(stringResource("filepathTF.prompt")) },
                    modifier = Modifier.weight(1f)
                )
                Text("O")
            }
            Divider(color = Color(0xFFDFDFDF), thickness = 1.dp)
        }

        // Main Content
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Pane
            LeftPane(selectedTab) { newTab -> selectedTab = newTab }

            // Center Pane
            Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> BookmarkTab()
                        1 -> TocGeneratorTab()
                        2 -> PageLabelTabView()
                        3 -> PdfPreviewTabView()
                    }
                }
            }
        }
        MessageContainer(messageContainerState)
    }
}
