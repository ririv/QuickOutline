package com.ririv.quickoutline.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ririv.quickoutline.pdfProcess.PdfPreview
import com.ririv.quickoutline.state.CurrentFileState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage

class ThumbnailViewModel(private val currentFileState: CurrentFileState) {
    var thumbnails by mutableStateOf<Map<Int, BufferedImage>>(emptyMap())
    var pageCount by mutableStateOf(0)
    private var pdfPreview: PdfPreview? = null

    init {
        CoroutineScope(Dispatchers.Swing).launch {
            currentFileState.srcFile.collectLatest { path ->
                pdfPreview?.close()
                pdfPreview = null
                thumbnails = emptyMap()
                pageCount = 0
                if (path != null) {
                    launch(Dispatchers.IO) {
                        try {
                            val preview = PdfPreview(path.toFile())
                            pdfPreview = preview
                            withContext(Dispatchers.Swing) {
                                pageCount = preview.pageCount
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun loadThumbnail(index: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            if (thumbnails[index] == null) {
                pdfPreview?.let { preview ->
                    try {
                        val image = preview.renderImage(index, 72f)
                        withContext(Dispatchers.Swing) {
                            val newThumbnails = thumbnails.toMutableMap()
                            newThumbnails[index] = image
                            thumbnails = newThumbnails
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
