package com.ririv.quickoutline.view.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.graphics.Color
import com.ririv.quickoutline.view.icons.AppIcon
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.controls.MessageContainer
import com.ririv.quickoutline.view.controls.MessageContainerState
import com.ririv.quickoutline.view.controls.GraphIconButton
import com.ririv.quickoutline.view.ui.bookmarktab.BookmarkTab
import com.ririv.quickoutline.view.viewmodel.MainViewModel
import org.koin.java.KoinJavaComponent.inject
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.net.URI
import java.net.URL

@Composable
fun MainView() {
//    val bookmarkViewModel: BookmarkViewModel by inject(BookmarkViewModel::class.java)
    val mainViewModel: MainViewModel by inject(MainViewModel::class.java)
    val messageContainerState: MessageContainerState by inject(MessageContainerState::class.java)
    var selectedTab by remember { mutableStateOf(0) }
    val uiState by mainViewModel.uiState.collectAsState()

    // Compose 原生 DnD：在根容器接收外部拖拽（Finder 等）
    // 仅提取第一个 .pdf 并打开，带简单去重防止重复打开
    val lastOpenedPath = remember { mutableStateOf("") }

    // 工具：解析 text/uri-list
    fun parseUriList(text: String): List<File> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { runCatching { URI(it) }.getOrNull() }
            .filter { it.scheme == "file" }
            .mapNotNull { runCatching { File(it) }.getOrNull() }
            .toList()

    // 工具：从 Transferable 提取第一个 PDF 文件
    fun extractPdf(t: Transferable): File? {
        val uriListFlavor = DataFlavor("text/uri-list;class=java.lang.String")
        val javaUrlFlavor = DataFlavor("application/x-java-url; class=java.net.URL")

        if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            val data = t.getTransferData(DataFlavor.javaFileListFlavor)
            if (data is List<*>) {
                val f = data.firstOrNull { it is File && (it as File).extension.equals("pdf", true) } as? File
                if (f != null) return f
            }
        }
        if (t.isDataFlavorSupported(javaUrlFlavor)) {
            val url = runCatching { t.getTransferData(javaUrlFlavor) as? URL }.getOrNull()
            val f = url?.let { runCatching { File(it.toURI()) }.getOrNull() }
            if (f != null && f.extension.equals("pdf", true)) return f
        }
        if (t.isDataFlavorSupported(uriListFlavor)) {
            val s = runCatching { t.getTransferData(uriListFlavor) as? String }.getOrNull()
            val files = s?.let { parseUriList(it) } ?: emptyList()
            val f = files.firstOrNull { it.extension.equals("pdf", true) }
            if (f != null) return f
        }
        if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            val s = runCatching { t.getTransferData(DataFlavor.stringFlavor) as? String }.getOrNull()
            if (!s.isNullOrBlank()) {
                val filesFromUris = parseUriList(s)
                val f1 = filesFromUris.firstOrNull { it.extension.equals("pdf", true) }
                if (f1 != null) return f1
                val f2 = s.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .map { File(it) }
                    .firstOrNull { it.extension.equals("pdf", true) && it.exists() }
                if (f2 != null) return f2
            }
        }
        return null
    }

    @OptIn(ExperimentalComposeUiApi::class)
    val dndModifier = Modifier.dragAndDropTarget(
        // 桌面端：直接接受，后续在 onDrop 中解析并过滤
        shouldStartDragAndDrop = { _: DragAndDropEvent -> true },
        target = object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val t = event.awtTransferable ?: return false
                val pdf = extractPdf(t) ?: return false
                val path = pdf.absolutePath
                if (lastOpenedPath.value == path) return true
                println("[DnD][Compose] opening PDF: $path")
                lastOpenedPath.value = path
                // 直接调用 ViewModel，与按钮一致
                mainViewModel.openPdf(path)
                return true
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize().then(dndModifier)) { // Use Box for layering
        // Main Content
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Pane
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF2F2F2))) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(30.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(76.dp)) // Space for traffic lights
                    if (uiState.paths.src_file != null) {
                        Text(
                        text = uiState.paths.src_file?.fileName?.toString() ?: "",
                        modifier = Modifier.weight(1f),
                        color = Color(100, 100, 100)
                        )
                    } else {
                        Text(
                            text = stringResource("filepathTF.prompt"),
                            modifier = Modifier.weight(1f),
                            color = Color(100, 100, 100, 120),
                        )
                    }
                    GraphIconButton(onClick = {
                        val dialog = FileDialog(null as Frame?, "Select File to Open", FileDialog.LOAD)
                        dialog.isVisible = true
                        val file = dialog.file
                        val dir = dialog.directory
                        if (file != null && dir != null) {
                            val path = dir + file
                            mainViewModel.openPdf(path)
                        }
                    }, modifier = Modifier.size(40.dp)) {
                        AppIcon(icon = AppIcon.Open, contentDescription = "Open file", modifier = Modifier.size(24.dp), tint = Color.Gray)
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFDFDFDF))
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