package com.ririv.quickoutline.view.ui.controls

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
        animationSpec = if (isDeselection || !isPressStateChanged) snap() else tween(),
        label = "backgroundColorAnimation"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
