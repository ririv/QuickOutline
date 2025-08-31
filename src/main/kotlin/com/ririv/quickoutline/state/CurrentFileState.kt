package com.ririv.quickoutline.state

import com.ririv.quickoutline.exception.EncryptedPdfException
import com.ririv.quickoutline.service.PdfOutlineService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.nio.file.Path

class CurrentFileState(private val pdfOutlineService: PdfOutlineService) {

    private val _srcFile = MutableStateFlow<Path?>(null)
    val srcFile = _srcFile.asStateFlow()

    private val _destFile = MutableStateFlow<Path?>(null)
    val destFile = _destFile.asStateFlow()

    fun setSrcFile(file: Path?) {
        if (file == null) {
            clear()
            return
        }
        // Centralized validation
        try {
            pdfOutlineService.checkOpenFile(file.toString())
            _srcFile.value = file
            _destFile.value = calculateDestFilePath(file)
        } catch (e: IOException) {
            // handle exception
        } catch (e: EncryptedPdfException) {
            // handle exception
        } catch (e: com.itextpdf.io.exceptions.IOException) {
            // handle exception
        }
    }

    private fun calculateDestFilePath(srcFilePath: Path?): Path? {
        if (srcFilePath == null) {
            return null
        }
        val srcFileName = srcFilePath.fileName.toString()
        val dotIndex = srcFileName.lastIndexOf(".")
        val nameWithoutExt = if (dotIndex == -1) srcFileName else srcFileName.substring(0, dotIndex)
        val ext = if (dotIndex == -1) "" else srcFileName.substring(dotIndex)

        val parentDir = srcFilePath.parent
        val destFileName = nameWithoutExt + "_new" + ext
        return parentDir.resolve(destFileName)
    }

    fun clear() {
        _srcFile.value = null
        _destFile.value = null
    }
}
