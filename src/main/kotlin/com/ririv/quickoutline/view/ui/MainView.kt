package com.ririv.quickoutline.view.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.controls.MessageContainer
import com.ririv.quickoutline.view.controls.MessageContainerState
import com.ririv.quickoutline.view.ui.bookmarktab.BookmarkTab
import com.ririv.quickoutline.view.viewmodel.MainViewModel
import org.koin.java.KoinJavaComponent.inject
import java.awt.FileDialog
import java.awt.Frame

@Composable
fun MainView() {
//    val bookmarkViewModel: BookmarkViewModel by inject(BookmarkViewModel::class.java)
    val mainViewModel: MainViewModel by inject(MainViewModel::class.java)
    val messageContainerState: MessageContainerState by inject(MessageContainerState::class.java)
    var selectedTab by remember { mutableStateOf(0) }
    val uiState by mainViewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) { // Use Box for layering
        // Main Content
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Pane
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF2F2F2))) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = uiState.paths.src_file?.toString() ?: "",
                        onValueChange = { },
                        enabled = false,
                        placeholder = { Text(stringResource("filepathTF.prompt")) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.textFieldColors(
                            disabledTextColor = Color(100, 100, 100),
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = {
                        val dialog = FileDialog(null as Frame?, "Select File to Open", FileDialog.LOAD)
                        dialog.isVisible = true
                        val file = dialog.file
                        val dir = dialog.directory
                        if (file != null && dir != null) {
                            val path = dir + file
                            mainViewModel.openPdf(path)
                        }
                    }) {
                        Icon(
                            painter = loadResourcePainter("drawable/open.svg"),
                            contentDescription = "Open file",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Gray
                        )
                    }
                }
                Divider(color = Color(0xFFDFDFDF), thickness = 1.dp)
            }

            // Main Content Row
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
        }

        // Message container overlaid on top
        MessageContainer(
            state = messageContainerState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        )
    }
}