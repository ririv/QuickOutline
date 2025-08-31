package com.ririv.quickoutline.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.controls.StyledSlider
import org.koin.java.KoinJavaComponent.inject

@Composable
fun ThumbnailPane() {
    val viewModel: ThumbnailViewModel by inject(ThumbnailViewModel::class.java)
    var zoom by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        viewModel.loadThumbnails()
    }

    Column(modifier = Modifier.padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Z-") // Placeholder for icon
            StyledSlider(value = zoom, onValueChange = { zoom = it }, valueRange = 0.5f..3f, modifier = Modifier.weight(1f))
            Text("Z+") // Placeholder for icon
        }
        LazyVerticalGrid(columns = GridCells.Adaptive(100.dp)) {
            items(viewModel.thumbnails) { thumbnail ->
                Image(
                    bitmap = thumbnail.toComposeImageBitmap(),
                    contentDescription = null
                )
            }
        }
    }
}
