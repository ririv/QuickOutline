package com.ririv.quickoutline.view.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ririv.quickoutline.service.PdfTocExtractorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TocGeneratorViewModel(
    private val pdfTocExtractorService: PdfTocExtractorService,
    private val mainViewModel: MainViewModel
) {
    var generatedToc by mutableStateOf("")
    var isGenerating by mutableStateOf(false)
    var status by mutableStateOf("")

    fun generateToc() {
        CoroutineScope(Dispatchers.IO).launch {
            isGenerating = true
            status = "Generating..."
            try {
                val filePath = mainViewModel.uiState.value.paths.source?.toString() ?: return@launch
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
