package com.ririv.quickoutline.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.ririv.quickoutline.view.viewmodel.PdfPreviewViewModel
import org.koin.java.KoinJavaComponent.inject

@Composable
fun PdfPreviewTabView() {
    val viewModel: PdfPreviewViewModel by inject(PdfPreviewViewModel::class.java)
    val images by remember(viewModel) { derivedStateOf { viewModel.images } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(images) { image ->
            Image(
                bitmap = image.toComposeImageBitmap(),
                contentDescription = null
            )
        }
    }
}