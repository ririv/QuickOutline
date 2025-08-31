package com.ririv.quickoutline.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class LeftPaneViewModel {
    var files by mutableStateOf<List<File>>(emptyList())

    fun loadFiles(path: String) {
        val file = File(path)
        files = file.listFiles()?.toList() ?: emptyList()
    }
}
