package com.ririv.quickoutline.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.ririv.quickoutline.pdfProcess.PdfPreview
import com.ririv.quickoutline.service.PdfPageLabelService
import com.ririv.quickoutline.state.CurrentFileState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.min

private const val BATCH_SIZE = 20

class ThumbnailViewModel(
    private val currentFileState: CurrentFileState,
    private val pageLabelService: PdfPageLabelService
) {
    var thumbnails by mutableStateOf<Map<Int, ImageBitmap>>(emptyMap())
    var pageCount by mutableStateOf(0)
    var itemsToRender = mutableStateListOf<Int>()
        private set
    var pageLabels by mutableStateOf<List<String>>(emptyList())
        private set

    private var pdfPreview: PdfPreview? = null
    private var isLoading by mutableStateOf(false)

    init {
        CoroutineScope(Dispatchers.Swing).launch {
            currentFileState.uiState.collectLatest { uiState ->
                val path = uiState.paths.source
                pdfPreview?.close()
                pdfPreview = null
                thumbnails = emptyMap()
                pageCount = 0
                itemsToRender.clear()
                pageLabels = emptyList()

                if (path != null) {
                    launch(Dispatchers.IO) {
                        try {
                            val preview = PdfPreview(path.toFile())
                            val labels = try {
                                pageLabelService.getPageLabels(path.toString()).toList()
                            } catch (e: IOException) {
                                e.printStackTrace()
                                emptyList()
                            }

                            pdfPreview = preview
                            withContext(Dispatchers.Swing) {
                                pageCount = preview.pageCount
                                pageLabels = labels
                                loadMore()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun loadMore() {
        if (isLoading || itemsToRender.size >= pageCount) return
        isLoading = true
        val currentSize = itemsToRender.size
        val newCount = min(currentSize + BATCH_SIZE, pageCount)
        for (i in currentSize until newCount) {
            itemsToRender.add(i)
        }
        isLoading = false
    }

    fun loadThumbnail(index: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            if (thumbnails[index] == null) {
                pdfPreview?.let { preview ->
                    try {
                        val bufferedImage = preview.renderImage(index, 150f)
                        val imageBitmap = bufferedImage.toComposeImageBitmap()
                        withContext(Dispatchers.Swing) {
                            val newThumbnails = thumbnails.toMutableMap()
                            newThumbnails[index] = imageBitmap
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
