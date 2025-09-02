package com.ririv.quickoutline.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ririv.quickoutline.view.controls.StyledSlider
import com.ririv.quickoutline.view.icons.SvgIcon
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.koin.java.KoinJavaComponent.inject
import kotlin.math.max

private val BASE_WIDTH = 150.dp
private val BASE_HEIGHT = 225.dp

@Composable
fun ThumbnailPane() {
    val viewModel: ThumbnailViewModel by inject(ThumbnailViewModel::class.java)
    val thumbnails by viewModel::thumbnails
    val itemsToRender = viewModel.itemsToRender
    val pageLabels by viewModel::pageLabels
    var zoom by remember { mutableStateOf(1f) }

    val thumbnailWidth = remember(zoom) { BASE_WIDTH * zoom }
    val thumbnailHeight = remember(zoom) { BASE_HEIGHT * zoom }

    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState, itemsToRender) {
        snapshotFlow { gridState.layoutInfo }
            .map { it.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filterNotNull()
            .collect { lastIndex ->
                if (lastIndex >= itemsToRender.size - 5) {
                    viewModel.loadMore()
                }
            }
    }

    Column(modifier = Modifier.padding(10.dp).fillMaxHeight()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
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
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val columnCount = max(1, (maxWidth / (thumbnailWidth + 20.dp)).toInt())

            Box {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = itemsToRender,
                        key = { index -> index }
                    ) { index ->
                        // This DisposableEffect is the core of the cancellation logic.
                        // It will call the cancellation function when the item leaves the composition.
                        DisposableEffect(index) {
                            onDispose {
                                viewModel.cancelThumbnailJob(index)
                            }
                        }

                        val thumbnail = thumbnails[index]

                        val labelText = if (pageLabels.isNotEmpty() && index < pageLabels.size && pageLabels[index].isNotBlank()) {
                            pageLabels[index]
                        } else {
                            (index + 1).toString()
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // The "photo paper" Box with the shadow
                            Box(
                                modifier = Modifier
                                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(4.dp))
                                    .background(Color.White, RoundedCornerShape(4.dp))
                            ) {
                                if (thumbnail != null) {
                                    Image(
                                        bitmap = thumbnail,
                                        contentDescription = "Thumbnail for page ${index + 1}",
                                        modifier = Modifier.size(thumbnailWidth, thumbnailHeight).clip(RoundedCornerShape(2.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.size(thumbnailWidth, thumbnailHeight).background(Color.LightGray))
                                    // Load the thumbnail when the placeholder is composed
                                    LaunchedEffect(index) {
                                        viewModel.loadThumbnail(index)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = labelText, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = gridState
                    )
                )
            }
        }
    }
}
