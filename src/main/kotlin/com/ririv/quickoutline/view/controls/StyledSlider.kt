package com.ririv.quickoutline.view.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun StyledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    theme: String = "default",
    // 额外可调参数帮助排查与适配：
    fillThumb: Boolean = true, // 默认填充内部，使其成为白色（或主题 surface）实心圆环
    thumbFillColor: Color = Color.Unspecified, // 若指定则用该颜色
    showDebugLayout: Boolean = false,
    materialFallback: Boolean = false // true 使用原 material slider 方便对比
) {
    val thumbAndTrackColor = when (theme) {
        "success" -> Color(0xFF52C41A)
        "warning" -> Color(0xFFFAAD14)
        "error" -> Color(0xFFF56C6C)
        else -> Color(0xFF409EFF)
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.Transparent, // 自定义 thumb 自己绘制
        activeTrackColor = thumbAndTrackColor,
        inactiveTrackColor = thumbAndTrackColor.copy(alpha = 0.24f),
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )

    val interactionSource = remember { MutableInteractionSource() }

    if (materialFallback) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = sliderColors,
                modifier = modifier
                    .indication(interactionSource, indication = null)
                    .let { if (showDebugLayout) it.border(1.dp, Color.Magenta) else it },
                enabled = enabled,
                interactionSource = interactionSource,
                track = { sliderState ->
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .fillMaxWidth()
                            .let { if (showDebugLayout) it.border(1.dp, Color.Cyan) else it },
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
                    val effectiveFill = when {
                        fillThumb && thumbFillColor != Color.Unspecified -> thumbFillColor
                        fillThumb -> MaterialTheme.colorScheme.surface // 之前为透明导致看到轨道
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .size(thumbSize)
                            .then(if (showDebugLayout) Modifier.border(1.dp, Color.Yellow) else Modifier)
                            .clip(CircleShape)
                            .background(effectiveFill, CircleShape)
                            .border(
                                BorderStroke(borderWidth, thumbAndTrackColor),
                                CircleShape
                            )
                    )
                }
            )
        }
        return
    }

    // ========= 自绘实现 =========
    val trackHeight = 5.dp
    val baseThumbSize = 14.dp
    val hoverThumbSize = 18.dp
    var isHovered by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val fraction = if (valueRange.endInclusive - valueRange.start == 0f) 0f else
        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    val thumbSize by animateDpAsState(if (isHovered || isDragging) hoverThumbSize else baseThumbSize)
    val borderWidth by animateDpAsState(if (isHovered || isDragging) 3.dp else 2.dp)

    val effectiveFill = when {
        fillThumb && thumbFillColor != Color.Unspecified -> thumbFillColor
        fillThumb -> MaterialTheme.colorScheme.surface
        else -> Color.Transparent
    }

    fun positionToValue(localX: Float): Float {
        val w = boxSize.width.toFloat().coerceAtLeast(1f)
        val clamped = localX.coerceIn(0f, w)
        val newV = valueRange.start + (clamped / w) * (valueRange.endInclusive - valueRange.start)
        if (steps > 0) {
            val stepSize = (valueRange.endInclusive - valueRange.start) / (steps + 1)
            val snapped = (kotlin.math.round((newV - valueRange.start) / stepSize) * stepSize) + valueRange.start
            return snapped.coerceIn(valueRange.start, valueRange.endInclusive)
        }
        return newV.coerceIn(valueRange.start, valueRange.endInclusive)
    }

    Box(
        modifier = modifier
            .onSizeChanged { boxSize = it }
            .height(hoverThumbSize)
            .fillMaxWidth()
            .let { if (showDebugLayout) it.border(1.dp, Color.Magenta) else it }
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset: Offset ->
                        isDragging = true
                        val nv = positionToValue(offset.x)
                        if (nv != value) onValueChange(nv)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        val nv = positionToValue(change.position.x)
                        if (nv != value) onValueChange(nv)
                        change.consume()
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(trackHeight)
                .let { if (showDebugLayout) it.border(1.dp, Color.Cyan) else it }
        ) {
            val w = size.width
            val h = size.height
            val activeW = w * fraction
            drawRoundRect(
                color = thumbAndTrackColor.copy(alpha = 0.24f),
                size = androidx.compose.ui.geometry.Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
            )
            drawRoundRect(
                color = thumbAndTrackColor,
                size = androidx.compose.ui.geometry.Size(activeW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
            )
        }

        val density = LocalDensity.current
        val thumbOffsetX = (boxSize.width * fraction) - with(density) { thumbSize.toPx() } / 2f
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetX.toInt(), 0) }
                .align(Alignment.CenterStart)
                .size(thumbSize)
                .then(if (showDebugLayout) Modifier.border(1.dp, Color.Yellow) else Modifier)
                .clip(CircleShape)
                .background(effectiveFill, CircleShape)
                .border(
                    BorderStroke(borderWidth, thumbAndTrackColor),
                    CircleShape
                )
        )
    }
}
