package com.ririv.quickoutline.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.controls.StyledSlider
import com.ririv.quickoutline.view.icons.SvgIcon
import org.koin.java.KoinJavaComponent.inject

@Composable
fun ThumbnailPane() {
    val viewModel: ThumbnailViewModel by inject(ThumbnailViewModel::class.java)
    val thumbnails by remember(viewModel) { derivedStateOf { viewModel.thumbnails } }
    var zoom by remember { mutableStateOf(1f) }

    Column(modifier = Modifier.padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SvgIcon(
                resource = "drawable/特色-风景.svg",
                modifier = Modifier.size(12.dp),
                tint = Color.Gray
            )
            StyledSlider(value = zoom, onValueChange = { zoom = it }, valueRange = 0.5f..3f, modifier = Modifier.weight(1f))
            SvgIcon(
                resource = "drawable/特色-风景.svg",
                modifier = Modifier.size(17.dp),
                tint = Color.Gray
            )
        }
        LazyVerticalGrid(columns = GridCells.Adaptive(100.dp)) {
            items(thumbnails) { thumbnail ->
                Image(
                    bitmap = thumbnail.toComposeImageBitmap(),
                    contentDescription = null
                )
            }
        }
    }
}
