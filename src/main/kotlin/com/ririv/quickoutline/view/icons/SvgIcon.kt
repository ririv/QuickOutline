package com.ririv.quickoutline.view.icons

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ririv.quickoutline.view.loadResourcePainter

@Composable
fun SvgIcon(
    resource: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    Icon(
        painter = loadResourcePainter(resource),
        contentDescription = null,
        modifier = modifier,
        tint = tint
    )
}
