package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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



    val backgroundColor = when (type) {
        ButtonType.PRIMARY -> when {
            isPressed -> Color(0xFF3375E0)
            isHovered -> Color(0xFF66B1FF)
            else -> Color(0xFF409EFF)
        }
        ButtonType.DEFAULT -> when {
            isPressed -> Color(0xFFECF5FF)
            isHovered -> Color(0xFFECF5FF)
            else -> Color.White
        }
        ButtonType.PLAIN_PRIMARY -> when {
            isPressed -> Color(0xFF3375E0)
            isHovered -> Color(0xFF409EFF)
            else -> Color(0xFFECF5FF)
        }
        ButtonType.PLAIN_IMPORTANT -> when {
            isPressed -> Color(0xFFD9534F)
            isHovered -> Color(0xFFF56C6C)
            else -> Color(0xFFFEF0F0)
        }
    }

    val contentColor = when (type) {
        ButtonType.PRIMARY -> Color.White
        ButtonType.DEFAULT -> if (isHovered || isPressed) Color(0xFF409EFF) else Color(0xFF606266)
        ButtonType.PLAIN_PRIMARY -> if (isHovered || isPressed) Color.White else Color(0xFF409EFF)
        ButtonType.PLAIN_IMPORTANT -> if (isHovered || isPressed) Color.White else Color(0xFFF56C6C)
    }

    val borderColor = when (type) {
        ButtonType.DEFAULT -> when {
            isPressed -> Color(0xFFB3D8FF)
            isHovered -> Color.Transparent
            else -> Color(0xFFDCDFE6)
        }
        ButtonType.PLAIN_PRIMARY -> when {
            isPressed -> Color(0xFF3375E0)
            isHovered -> Color(0xFF409EFF)
            else -> Color(0xFFB3D8FF)
        }
        ButtonType.PLAIN_IMPORTANT -> when {
            isPressed -> Color(0xFFD9534F)
            isHovered -> Color(0xFFF56C6C)
            else -> Color(0xFFFBC4C4)
        }
        else -> Color.Transparent
    }

//    Box(
//        modifier = modifier
//            .clip(RoundedCornerShape(4.dp))
//            .background(backgroundColor)
//            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
//            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
//            .padding(horizontal = 16.dp, vertical = 8.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(text, color = contentColor)
//    }


    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(4.dp),

        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor, // 主题色
            contentColor = contentColor // 内容颜色
        ),
//        modifier = Modifier
//            .padding(top = 8.dp) // 外边距 (margin)
//            .height(35.dp),       // 设置固定高度
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ){
        Text(text, color = contentColor)
    }
}
