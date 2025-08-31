package com.ririv.quickoutline.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ririv.quickoutline.exception.NoOutlineException
import com.ririv.quickoutline.model.Bookmark
import com.ririv.quickoutline.service.PdfOutlineService
import com.ririv.quickoutline.state.BookmarkSettingsState
import com.ririv.quickoutline.state.CurrentFileState
import com.ririv.quickoutline.textProcess.TextProcessor
import com.ririv.quickoutline.textProcess.methods.Method
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class BookmarkViewModel(
    private val pdfOutlineService: PdfOutlineService,
    private val currentFileState: CurrentFileState,
    private val bookmarkSettingsState: BookmarkSettingsState
) {
    val bookmarks = bookmarkSettingsState.rootBookmark
    var selectedBookmark by mutableStateOf<Bookmark?>(null)
    var filePath by mutableStateOf("")

    init {
        CoroutineScope(Dispatchers.Swing).launch {
            currentFileState.srcFile.collectLatest { path ->
                filePath = path?.toString() ?: ""
                if (path != null) {
                    launch(Dispatchers.IO) {
                        try {
                            val newRootBookmark = pdfOutlineService.getOutlineAsBookmark(path.toString(), 0)
                            withContext(Dispatchers.Swing) {
                                bookmarkSettingsState.setRootBookmark(newRootBookmark)
                            }
                        } catch (e: NoOutlineException) {
                            e.printStackTrace()
                            withContext(Dispatchers.Swing) {
                                bookmarkSettingsState.setRootBookmark(null)
                            }
                        }
                    }
                } else {
                    bookmarkSettingsState.setRootBookmark(null)
                }
            }
        }
    }

    fun openPdf(path: String) {
        currentFileState.setSrcFile(java.io.File(path).toPath())
    }

    fun addBookmark(title: String) {
        val newBookmark = Bookmark(title, 0, selectedBookmark?.level?.plus(1) ?: 1)
        val parent = selectedBookmark ?: bookmarkSettingsState.rootBookmark.value ?: return
        parent.addChild(newBookmark)
        // The view should observe the rootBookmark and update itself
    }

    fun editBookmark(newTitle: String) {
        selectedBookmark?.title = newTitle
        // The view should observe the rootBookmark and update itself
    }

    fun deleteBookmark() {
        selectedBookmark?.let { bookmark ->
            bookmark.parent?.children?.remove(bookmark)
            // The view should observe the rootBookmark and update itself
        }
    }

    fun updateBookmarksFromText(text: String) {
        val textProcessor = TextProcessor()
        val newRootBookmark = textProcessor.process(text, 0, Method.SEQ)
        bookmarkSettingsState.setRootBookmark(newRootBookmark)
    }
}