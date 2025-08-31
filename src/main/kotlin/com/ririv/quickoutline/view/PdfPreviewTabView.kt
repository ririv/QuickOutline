package com.ririv.quickoutline.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.koin.java.KoinJavaComponent.inject

@Composable
fun PdfPreviewTabView() {
    val viewModel: PdfPreviewViewModel by inject(PdfPreviewViewModel::class.java)

    LaunchedEffect(Unit) {
        viewModel.loadPdf()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(viewModel.images) { image ->
            Image(
                bitmap = image.toComposeImageBitmap(),
                contentDescription = null
            )
        }
    }
}
