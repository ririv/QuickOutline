package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.AppColors

enum class ButtonType {
    PRIMARY, DEFAULT, PLAIN_PRIMARY, PLAIN_IMPORTANT
}

@Composable
fun StyledButton(
    onClick: () -> Unit,
    text: String,
    type: ButtonType = ButtonType.DEFAULT,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonStateColors = when (type) {
        ButtonType.PRIMARY -> AppColors.primaryButtonColors
        ButtonType.DEFAULT -> AppColors.defaultButtonColors
        ButtonType.PLAIN_PRIMARY -> AppColors.plainPrimaryButtonColors
        ButtonType.PLAIN_IMPORTANT -> AppColors.plainImportantButtonColors
    }

    val colors = when {
        isPressed -> buttonStateColors.pressed
        isHovered -> buttonStateColors.hovered
        else -> buttonStateColors.default
    }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        interactionSource = interactionSource,
        border = BorderStroke(1.dp, colors.borderColor),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.containerColor,
            contentColor = colors.contentColor
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, color = colors.contentColor)
    }
}