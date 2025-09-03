package com.ririv.quickoutline.view.state

import java.nio.file.Path

data class FilePaths(
    val source: Path?,
    val destination: Path?
)

data class CurrentFileUiState(
    val paths: FilePaths = FilePaths(null, null),
    val error: String? = null
)
