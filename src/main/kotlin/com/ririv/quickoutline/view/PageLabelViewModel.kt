package com.ririv.quickoutline.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ririv.quickoutline.pdfProcess.PageLabel
import com.ririv.quickoutline.service.PdfPageLabelService
import com.ririv.quickoutline.state.CurrentFileState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class PageLabelViewModel(
    private val pdfPageLabelService: PdfPageLabelService,
    private val currentFileState: CurrentFileState
) {
    var pageLabels by mutableStateOf("")
    var numberingStyle by mutableStateOf(PageLabel.PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS)
    var prefix by mutableStateOf("")
    var startNumber by mutableStateOf("1")
    var fromPage by mutableStateOf("1")
    var rules by mutableStateOf<List<PageLabel>>(emptyList())

    init {
        CoroutineScope(Dispatchers.Swing).launch {
            currentFileState.uiState.collectLatest { uiState ->
                if (uiState.paths.source != null) {
                    loadPageLabels()
                } else {
                    pageLabels = ""
                    rules = emptyList()
                }
            }
        }
    }

    private fun loadPageLabels() {
        CoroutineScope(Dispatchers.IO).launch {
            val filePath = currentFileState.uiState.value.paths.source?.toString() ?: return@launch
            val labels = pdfPageLabelService.getPageLabels(filePath)
            withContext(Dispatchers.Swing) {
                pageLabels = labels.joinToString("\n")
            }
        }
    }

    fun addRule() {
        val newRule = PageLabel(
            fromPage.toInt(),
            numberingStyle,
            prefix,
            startNumber.toInt()
        )
        rules = rules + newRule
    }

    fun setPageLabels() {
        CoroutineScope(Dispatchers.IO).launch {
            val srcFilePath = currentFileState.uiState.value.paths.source?.toString() ?: return@launch
            val destFilePath = currentFileState.uiState.value.paths.destination?.toString() ?: return@launch
            pdfPageLabelService.setPageLabels(srcFilePath, destFilePath, rules)
        }
    }
}