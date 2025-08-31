package com.ririv.quickoutline.view.controls

import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun StyledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    theme: String = "default"
) {
    val colors = when (theme) {
        "success" -> SliderDefaults.colors(
            thumbColor = Color(0xFF52C41A),
            activeTrackColor = Color(0xFF52C41A)
        )
        "warning" -> SliderDefaults.colors(
            thumbColor = Color(0xFFFAAD14),
            activeTrackColor = Color(0xFFFAAD14)
        )
        "error" -> SliderDefaults.colors(
            thumbColor = Color(0xFFF56C6C),
            activeTrackColor = Color(0xFFF56C6C)
        )
        else -> SliderDefaults.colors(
            thumbColor = Color(0xFF409EFF),
            activeTrackColor = Color(0xFF409EFF)
        )
    }

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = colors,
        modifier = modifier
    )
}
