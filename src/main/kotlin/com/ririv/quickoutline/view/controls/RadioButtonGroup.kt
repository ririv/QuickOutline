package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.AppColors

@Composable
private fun StyledRadioButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val colors = AppColors.plainPrimaryButtonColors
    val selectedColor = colors.default.containerColor // The light blue for selected background

    val backgroundColor = if (selected) selectedColor else Color.Transparent

    val textColor =
        if (selected) {
            colors.default.contentColor
        } else if (isHovered && !selected) {
            colors.default.contentColor
        } else {
            colors.default.contentColor
        }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color(0xFF409EFF)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor)
    }
}

@Composable
fun <T> RadioButtonGroup(
    items: List<Pair<String, T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = AppColors.plainPrimaryButtonColors.default.borderColor

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min) // Constrain height to the minimum intrinsic height of children
            .clip(RoundedCornerShape(6.dp)) // Clip the children to the rounded shape
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
    ) {
        items.forEachIndexed { index, (text, value) ->
            StyledRadioButton(
                text = text,
                selected = selectedItem == value,
                onClick = { onItemSelected(value) },
            )

            if (index < items.size - 1) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight().width(1.dp),
                    color = borderColor
                )
            }
        }
    }
}