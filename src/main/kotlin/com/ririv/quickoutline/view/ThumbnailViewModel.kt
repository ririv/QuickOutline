package com.ririv.quickoutline.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ririv.quickoutline.pdfProcess.PdfPreview
import com.ririv.quickoutline.state.CurrentFileState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage

class ThumbnailViewModel(private val currentFileState: CurrentFileState) {
    var thumbnails by mutableStateOf<List<BufferedImage>>(emptyList())

    fun loadThumbnails() {
        CoroutineScope(Dispatchers.IO).launch {
            val file = currentFileState.srcFile?.toFile() ?: return@launch
            val pdfPreview = PdfPreview(file)
            val pageCount = pdfPreview.pageCount
            val imageList = mutableListOf<BufferedImage>()
            for (i in 0 until pageCount) {
                pdfPreview.renderThumbnail(i) { image ->
                    imageList.add(image)
                }
            }
            thumbnails = imageList
        }
    }
}
