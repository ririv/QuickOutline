package com.ririv.quickoutline.view.controls

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
private fun <T> rememberPrevious(value: T): T? {
    val ref = remember { object { var value: T? = null } }
    SideEffect {
        ref.value = value
    }
    return ref.value
}

@Composable
private fun BaseGraphButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun GraphIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val wasPressed = rememberPrevious(isPressed)

    val targetBackgroundColor = when {
        isPressed -> Color(0xFFD0D0D0)
        isHovered -> Color(0xFFE0E0E0)
        else -> Color.Transparent
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
//        只有在“按下”和“松开”的瞬间，背景色才会有平滑的动画效果。对于鼠标的“悬浮”和“移走”，背景色是瞬间变化的。
        animationSpec = if (wasPressed != isPressed) tween() else snap(),
        label = "backgroundColorAnimation"
    )

    BaseGraphButton(
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        backgroundColor = backgroundColor,
        content = content
    )
}

@Composable
fun GraphIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val previousChecked = rememberPrevious(checked)
    val wasPressed = rememberPrevious(isPressed)

    val targetBackgroundColor = when {
        isPressed -> Color(0xFFD0D0D0)
        checked -> Color(0xFFE0E0E0)
        isHovered -> Color(0xFFE0E0E0)
        else -> Color.Transparent
    }

    val isDeselection = previousChecked == true && !checked
    val isPressStateChanged = wasPressed != isPressed

    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
//        * 当按下并选中按钮时，背景色会平滑地变色。
//        * 当取消选中时，背景色会瞬间变回去。
//        * 当鼠标悬浮或移走时，背景色也是瞬间变化的。
        animationSpec = if (isDeselection || !isPressStateChanged) snap() else tween(),
        label = "backgroundColorAnimation"
    )

    BaseGraphButton(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        interactionSource = interactionSource,
        backgroundColor = backgroundColor,
        content = content
    )
}