package com.ririv.quickoutline.view

import com.ririv.quickoutline.model.Bookmark
import com.ririv.quickoutline.pdfProcess.ViewScaleType
import com.ririv.quickoutline.service.PdfOutlineService
import com.ririv.quickoutline.state.CurrentFileState
import com.ririv.quickoutline.textProcess.TextProcessor
import com.ririv.quickoutline.textProcess.methods.Method
import com.ririv.quickoutline.view.controls.MessageContainerState
import com.ririv.quickoutline.view.controls.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class BookmarkViewModel(
    private val pdfOutlineService: PdfOutlineService,
    private val currentFileState: CurrentFileState,
    private val messageContainerState: MessageContainerState
) {
    private val _uiState = MutableStateFlow(BookmarkUiState())
    val uiState: StateFlow<BookmarkUiState> = _uiState.asStateFlow()

    init {
        CoroutineScope(Dispatchers.Swing).launch {
            currentFileState.uiState.collectLatest { fileUiState ->
                val path = fileUiState.paths.source
                _uiState.update { it.copy(filePath = path?.toString() ?: "") }
                if (path != null) {
                    loadBookmarks()
                } else {
                    _uiState.update { it.copy(rootBookmark = null) }
                }
            }
        }
    }

    fun openPdf(path: String) {
        currentFileState.setSrcFile(java.io.File(path).toPath())
    }

    fun loadBookmarks() {
        val path = currentFileState.uiState.value.paths.source ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newRootBookmark = pdfOutlineService.getOutlineAsBookmark(path.toString(), 0)
                withContext(Dispatchers.Swing) {
                    _uiState.update { it.copy(rootBookmark = newRootBookmark) }
                    messageContainerState.showMessage("Bookmarks loaded successfully.", MessageType.SUCCESS)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Swing) {
                    messageContainerState.showMessage("Failed to load bookmarks: ${e.message}", MessageType.ERROR)
                }
            }
        }
    }

    fun saveBookmarks() {
        val root = uiState.value.rootBookmark ?: return
        val sourcePath = currentFileState.uiState.value.paths.source?.toString() ?: return
        val destPath = currentFileState.uiState.value.paths.destination?.toString() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                pdfOutlineService.setOutline(root, sourcePath, destPath, ViewScaleType.NONE)
                withContext(Dispatchers.Swing) {
                    messageContainerState.showMessage("Bookmarks saved successfully!", MessageType.SUCCESS)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Swing) {
                    messageContainerState.showMessage("Failed to save bookmarks: ${e.message}", MessageType.ERROR)
                }
            }
        }
    }

    fun addBookmark(title: String) {
        val parent = uiState.value.selectedBookmark ?: uiState.value.rootBookmark ?: return
        val newBookmark = Bookmark(title, 0, parent.level + 1)
        parent.addChild(newBookmark)
        _uiState.update { it.copy(recomposeTrigger = it.recomposeTrigger + 1) }
    }

    fun editBookmark(newTitle: String) {
        uiState.value.selectedBookmark?.title = newTitle
        _uiState.update { it.copy(recomposeTrigger = it.recomposeTrigger + 1) }
    }

    fun deleteBookmark() {
        uiState.value.selectedBookmark?.let { bookmark ->
            bookmark.parent?.children?.remove(bookmark)
            _uiState.update { it.copy(recomposeTrigger = it.recomposeTrigger + 1) }
        }
    }

    fun updateBookmarksFromText(text: String) {
        val textProcessor = TextProcessor()
        val newRootBookmark = textProcessor.process(text, 0, Method.SEQ)
        _uiState.update { it.copy(rootBookmark = newRootBookmark) }
    }

    fun selectBookmark(bookmark: Bookmark?) {
        _uiState.update { it.copy(selectedBookmark = bookmark) }
    }

    fun setOffset(offset: Int?) {
        _uiState.update { it.copy(offset = offset) }
    }
}
