package com.ririv.quickoutline.view.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import com.ririv.quickoutline.exception.NoOutlineException
import com.ririv.quickoutline.model.Bookmark
import com.ririv.quickoutline.pdfProcess.ViewScaleType
import com.ririv.quickoutline.service.PdfOutlineService
import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService
import com.ririv.quickoutline.textProcess.TextProcessor
import com.ririv.quickoutline.textProcess.methods.Method
import com.ririv.quickoutline.view.state.BookmarkUiState
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

import com.ririv.quickoutline.service.PdfTocExtractorService

class BookmarkViewModel(
    private val pdfOutlineService: PdfOutlineService,
    private val mainViewModel: MainViewModel,
    private val messageContainerState: MessageContainerState,
    private val syncWithExternalEditorService: SyncWithExternalEditorService,
    private val pdfTocExtractorService: PdfTocExtractorService
) {
    private val _uiState = MutableStateFlow(BookmarkUiState())
    val uiState: StateFlow<BookmarkUiState> = _uiState.asStateFlow()

    init {
        CoroutineScope(Dispatchers.Swing).launch {
            mainViewModel.uiState.collectLatest { fileUiState ->
                val path = fileUiState.paths.src_file
                _uiState.update { it.copy(filePath = path?.toString() ?: "") }
                if (path != null) {
                    loadBookmarks()
                } else {
                    _uiState.update { it.copy(rootBookmark = null, offset = TextFieldValue(""), textInput = TextFieldValue("")) }
                }
            }
        }
    }


    fun loadBookmarks() {
        val path = mainViewModel.uiState.value.paths.src_file ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newRootBookmark = pdfOutlineService.getOutlineAsBookmark(path.toString(), 0)
                val newText = newRootBookmark?.toOutlineString() ?: ""
                withContext(Dispatchers.Swing) {
                    _uiState.update { it.copy(rootBookmark = newRootBookmark, textInput = TextFieldValue(newText)) }
                    messageContainerState.showMessage("Bookmarks loaded successfully.", MessageType.SUCCESS)
                }
            } catch (e: NoOutlineException) {
                withContext(Dispatchers.Swing) {
                    messageContainerState.showMessage("No existing bookmarks found in the PDF.", MessageType.INFO)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Swing) {
                    messageContainerState.showMessage("Failed to load bookmarks: ${e.message}", MessageType.ERROR)
                }
            }
        }
    }

    fun extractToc() {
        val path = mainViewModel.uiState.value.paths.src_file ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tocText = pdfTocExtractorService.extract(path.toString())
                withContext(Dispatchers.Swing) {
                    _uiState.update { it.copy(textInput = TextFieldValue(tocText)) }
                    messageContainerState.showMessage("TOC extracted successfully.", MessageType.SUCCESS)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Swing) {
                    messageContainerState.showMessage("Failed to extract TOC: ${e.message}", MessageType.ERROR)
                }
            }
        }
    }

    fun saveBookmarks(viewScaleType: ViewScaleType) {
        val root = uiState.value.rootBookmark ?: return
        val sourcePath = mainViewModel.uiState.value.paths.src_file?.toString() ?: return
        val destPath = mainViewModel.uiState.value.paths.dst_file?.toString() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                pdfOutlineService.setOutline(root, sourcePath, destPath, viewScaleType)
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

    fun onTextInputChange(newValue: TextFieldValue) {
        // Update the text field value first to keep the UI responsive
        _uiState.update { it.copy(textInput = newValue) }

        // Then, process the text to update the bookmark tree structure
        val textProcessor = TextProcessor()
        val newRootBookmark = textProcessor.process(newValue.text, 0, Method.SEQ) // Assuming SEQ method for now
        _uiState.update { it.copy(rootBookmark = newRootBookmark) }
    }

    fun selectBookmark(bookmark: Bookmark?) {
        _uiState.update { it.copy(selectedBookmark = bookmark) }
    }

    fun setOffset(offset: TextFieldValue?) {
        // Allow only numbers and a leading minus sign
        val newText = offset?.text?.filterIndexed { index, char ->
            char.isDigit() || (index == 0 && char == '-')
        }
        if (offset != null) {
            if (newText != offset.text) {
                _uiState.update { newText?.let { it1 -> offset.copy(text = it1) }?.let { it2 -> it.copy(offset = it2) }!! }
            } else {
                _uiState.update { it.copy(offset = offset) }
            }
        }
    }

    fun syncWithExternalEditor() {
        // Use the injected service
        syncWithExternalEditorService.exec(
            null, // coordinates are not used in the new implementation
            { fileText -> // onFileChanged
                _uiState.update { it.copy(textInput = TextFieldValue(fileText)) }
            },
            { // onSyncStart
                syncWithExternalEditorService.writeTemp(uiState.value.textInput.text)
                _uiState.update { it.copy(isSyncingWithEditor = true) }
            },
            { // onSyncEnd
                _uiState.update { it.copy(isSyncingWithEditor = false) }
            },
            { // onVSCodeNotFound
                messageContainerState.showMessage(
                    "VSCode not found. Please install it and ensure it's in your system's PATH.",
                    MessageType.WARNING
                )
            }
        )
    }

    fun autoFormat() {
        val currentText = uiState.value.textInput.text
        val formattedText = pdfOutlineService.autoFormat(currentText)
        _uiState.update { it.copy(textInput = TextFieldValue(formattedText)) }
    }
}
