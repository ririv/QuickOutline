package com.ririv.quickoutline.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ririv.quickoutline.service.PdfTocExtractorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.ririv.quickoutline.state.CurrentFileState

class TocGeneratorViewModel(
    private val pdfTocExtractorService: PdfTocExtractorService,
    private val currentFileState: CurrentFileState
) {
    var generatedToc by mutableStateOf("")
    var isGenerating by mutableStateOf(false)
    var status by mutableStateOf("")

    fun generateToc() {
        CoroutineScope(Dispatchers.IO).launch {
            isGenerating = true
            status = "Generating..."
            try {
                val filePath = currentFileState.uiState.value.paths.source?.toString() ?: return@launch
                val toc = pdfTocExtractorService.extract(filePath)
                generatedToc = toc
                status = "Generated successfully."
            } catch (e: Exception) {
                status = "Error: ${e.message}"
            } finally {
                isGenerating = false
            }
        }
    }
}
