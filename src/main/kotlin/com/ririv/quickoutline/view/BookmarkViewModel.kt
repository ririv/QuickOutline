package com.ririv.quickoutline.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ririv.quickoutline.exception.NoOutlineException
import com.ririv.quickoutline.model.Bookmark
import com.ririv.quickoutline.service.PdfOutlineService
import com.ririv.quickoutline.textProcess.TextProcessor
import com.ririv.quickoutline.textProcess.methods.Method
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarkViewModel(
    private val pdfOutlineService: PdfOutlineService,
    private val sharedViewModel: SharedViewModel
) {
    var bookmarks by mutableStateOf<List<Bookmark>>(emptyList())
    var selectedBookmark by mutableStateOf<Bookmark?>(null)
    var filePath by mutableStateOf("")
    private var rootBookmark: Bookmark? = null

    fun openPdf() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sharedViewModel.currentFileState.setSrcFile(java.io.File(filePath).toPath())
                rootBookmark = pdfOutlineService.getOutlineAsBookmark(filePath, 0)
                bookmarks = rootBookmark?.children?.toMutableList() ?: mutableListOf()
            } catch (e: NoOutlineException) {
                e.printStackTrace()
            }
        }
    }

    fun addBookmark(title: String) {
        val newBookmark = Bookmark(title, 0, selectedBookmark?.level?.plus(1) ?: 1)
        val parent = selectedBookmark ?: rootBookmark ?: return
        parent.addChild(newBookmark)
        bookmarks = rootBookmark?.children?.toMutableList() ?: mutableListOf()
    }

    fun editBookmark(newTitle: String) {
        selectedBookmark?.title = newTitle
        bookmarks = rootBookmark?.children?.toMutableList() ?: mutableListOf()
    }

    fun deleteBookmark() {
        selectedBookmark?.let { bookmark ->
            bookmark.parent?.children?.remove(bookmark)
            bookmarks = rootBookmark?.children?.toMutableList() ?: mutableListOf()
        }
    }

    fun updateBookmarksFromText(text: String) {
        val textProcessor = TextProcessor()
        rootBookmark = textProcessor.process(text, 0, Method.SEQ)
        bookmarks = rootBookmark?.children?.toMutableList() ?: mutableListOf()
    }
}


