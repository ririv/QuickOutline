package com.ririv.quickoutline.view.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    theme: String = "default"
) {
    val thumbAndTrackColor = when (theme) {
        "success" -> Color(0xFF52C41A)
        "warning" -> Color(0xFFFAAD14)
        "error" -> Color(0xFFF56C6C)
        else -> Color(0xFF409EFF)
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.Transparent,
        activeTrackColor = thumbAndTrackColor,
        inactiveTrackColor = thumbAndTrackColor.copy(alpha = 0.24f)
    )

    val interactionSource = remember { MutableInteractionSource() }

    // 直接在 Slider 外层通过 modifier 禁用 indication (ripple)。部分版本中 IndicationInstance 不对外暴露。
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = sliderColors,
        modifier = modifier.indication(interactionSource, indication = null),
        enabled = enabled,
        interactionSource = interactionSource,
        track = { sliderState ->
            Box(
                modifier = Modifier
                    .height(5.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = sliderColors,
                    enabled = enabled
                )
            }
        },
        thumb = { _ ->
            val isDragged by interactionSource.collectIsDraggedAsState()
            val isHovered by interactionSource.collectIsHoveredAsState()
            val borderWidth by animateDpAsState(if (isDragged || isHovered) 3.dp else 2.dp)
            val thumbSize by animateDpAsState(if (isDragged || isHovered) 18.dp else 14.dp)

            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .border(
                        BorderStroke(borderWidth, thumbAndTrackColor),
                        CircleShape
                    ),
            )
        }
    )
}
