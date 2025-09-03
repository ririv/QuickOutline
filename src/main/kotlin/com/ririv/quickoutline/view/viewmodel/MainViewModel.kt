package com.ririv.quickoutline.view.viewmodel

import com.ririv.quickoutline.exception.EncryptedPdfException
import com.ririv.quickoutline.service.PdfOutlineService
import com.ririv.quickoutline.view.state.CurrentFileUiState
import com.ririv.quickoutline.view.state.FilePaths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.nio.file.Path

class MainViewModel(private val pdfOutlineService: PdfOutlineService) {

    // 3. Single StateFlow for the UI state
    private val _uiState = MutableStateFlow(CurrentFileUiState())
    val uiState = _uiState.asStateFlow()

    fun setSrcFile(file: Path?) {
        if (file == null) {
            clear()
            return
        }

        try {
            pdfOutlineService.checkOpenFile(file.toString())
            val destFile = calculateDestFilePath(file)
            // Atomically update the state
            _uiState.update {
                it.copy(
                    paths = FilePaths(source = file, destination = destFile),
                    error = null
                )
            }
        } catch (e: EncryptedPdfException) {
            _uiState.update {
                it.copy(error = "文件已加密，无法打开。") // Example error message
            }
        } catch (e: com.itextpdf.io.exceptions.IOException) {
            _uiState.update {
                it.copy(error = "文件已损坏或格式不正确。") // Example error message
            }
        } catch (e: IOException) {
            _uiState.update {
                it.copy(error = "无法读取文件: ${e.message}") // Example error message
            }
        }
    }

    // Make this public so UI can dismiss the error
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun calculateDestFilePath(srcFilePath: Path): Path {
        val srcFileName = srcFilePath.fileName.toString()
        val dotIndex = srcFileName.lastIndexOf(".")
        val nameWithoutExt = if (dotIndex == -1) srcFileName else srcFileName.substring(0, dotIndex)
        val ext = if (dotIndex == -1) "" else srcFileName.substring(dotIndex)

        val parentDir = srcFilePath.parent
        val destFileName = nameWithoutExt + "_new" + ext
        return parentDir.resolve(destFileName)
    }

    fun clear() {
        _uiState.value = CurrentFileUiState() // Reset to initial state
    }
}