package com.ririv.quickoutline.view.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.ririv.quickoutline.pdfProcess.PageLabel
import com.ririv.quickoutline.service.PdfPageLabelService
import com.ririv.quickoutline.view.controls.MessageContainerState
import com.ririv.quickoutline.view.controls.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class PageLabelViewModel(
    private val pdfPageLabelService: PdfPageLabelService,
    private val mainViewModel: MainViewModel,
    private val messageContainerState: MessageContainerState
) {
    var pageLabels by mutableStateOf("")
    var numberingStyle by mutableStateOf(PageLabel.PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS)
    var prefix by mutableStateOf(TextFieldValue(""))
    var startNumber by mutableStateOf(TextFieldValue("1"))
    var fromPage by mutableStateOf(TextFieldValue("1"))
    var rules by mutableStateOf<List<PageLabel>>(emptyList())

    init {
        CoroutineScope(Dispatchers.Swing).launch {
            mainViewModel.uiState.collectLatest { uiState ->
                if (uiState.paths.src_file != null) {
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
            val filePath = mainViewModel.uiState.value.paths.src_file?.toString() ?: return@launch
            val labels = pdfPageLabelService.getPageLabels(filePath)
            withContext(Dispatchers.Swing) {
                pageLabels = labels.joinToString("\n")
            }
        }
    }

    fun addRule() {
        if (mainViewModel.uiState.value.paths.src_file == null) {
            messageContainerState.showMessage("Please open a PDF file first.", MessageType.WARNING)
            return
        }
        try {
            val from = fromPage.text.toInt()
            if (from <= 0) {
                messageContainerState.showMessage("Page number must be positive.", MessageType.ERROR)
                return
            }
            if (rules.any { it.pageNum == from }) {
                messageContainerState.showMessage("A rule for this page already exists.", MessageType.ERROR)
                return
            }

            val newRule = PageLabel(
                from,
                numberingStyle,
                prefix.text,
                startNumber.text.toInt()
            )
            rules = (rules + newRule).sortedBy { it.pageNum }
            // Clear fields after adding
            prefix = TextFieldValue("")
            startNumber = TextFieldValue("1")
            fromPage = TextFieldValue("1")
        } catch (e: NumberFormatException) {
            messageContainerState.showMessage("Invalid number format.", MessageType.ERROR)
        }
    }

    fun removeRule(rule: PageLabel) {
        rules = rules - rule
    }

    fun setPageLabels() {
        if (mainViewModel.uiState.value.paths.src_file == null) {
            messageContainerState.showMessage("Please open a PDF file first.", MessageType.WARNING)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val srcFilePath = mainViewModel.uiState.value.paths.src_file?.toString()!!
            val destFilePath = mainViewModel.uiState.value.paths.dst_file?.toString()!!
            try {
                pdfPageLabelService.setPageLabels(srcFilePath, destFilePath, rules)
                withContext(Dispatchers.Swing) {
                    messageContainerState.showMessage("Page labels applied successfully!", MessageType.SUCCESS)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Swing) {
                    messageContainerState.showMessage("Error applying page labels: ${e.message}", MessageType.ERROR)
                }
            }
        }
    }
}