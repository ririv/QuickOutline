package com.ririv.quickoutline.view.icons

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

@Composable
fun SvgIcon(
    resource: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    Icon(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = modifier,
        tint = tint
    )
}
