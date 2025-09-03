package com.ririv.quickoutline.view.state

import java.nio.file.Path

data class FilePaths(
    val src_file: Path?,
    val dst_file: Path?
)

data class CurrentFileUiState(
    val paths: FilePaths = FilePaths(null, null),
    val error: String? = null
)
