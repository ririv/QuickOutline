package com.ririv.quickoutline.view

import androidx.compose.ui.graphics.Color

object AppColors {

    data class ButtonColors(
        val containerColor: Color,
        val contentColor: Color,
        val borderColor: Color
    )

    data class ButtonStateColors(
        val default: ButtonColors,
        val hovered: ButtonColors,
        val pressed: ButtonColors
    )

    val primaryButtonColors = ButtonStateColors(
        default = ButtonColors(containerColor = Color(0xFF409EFF), contentColor = Color.White, borderColor = Color.Transparent),
        hovered = ButtonColors(containerColor = Color(0xFF66B1FF), contentColor = Color.White, borderColor = Color.Transparent),
        pressed = ButtonColors(containerColor = Color(0xFF3375E0), contentColor = Color.White, borderColor = Color.Transparent)
    )

    val defaultButtonColors = ButtonStateColors(
        default = ButtonColors(containerColor = Color.White, contentColor = Color(0xFF606266), borderColor = Color(0xFFDCDFE6)),
        hovered = ButtonColors(containerColor = Color(0xFFECF5FF), contentColor = Color(0xFF409EFF), borderColor = Color.Transparent),
        pressed = ButtonColors(containerColor = Color(0xFFECF5FF), contentColor = Color(0xFF409EFF), borderColor = Color(0xFFB3D8FF))
    )

    val plainPrimaryButtonColors = ButtonStateColors(
        default = ButtonColors(containerColor = Color(0xFFECF5FF), contentColor = Color(0xFF409EFF), borderColor = Color(0xFFB3D8FF)),
        hovered = ButtonColors(containerColor = Color(0xFF409EFF), contentColor = Color.White, borderColor = Color(0xFF409EFF)),
        pressed = ButtonColors(containerColor = Color(0xFF3375E0), contentColor = Color.White, borderColor = Color(0xFF3375E0))
    )

    val plainImportantButtonColors = ButtonStateColors(
        default = ButtonColors(containerColor = Color(0xFFFEF0F0), contentColor = Color(0xFFF56C6C), borderColor = Color(0xFFFBC4C4)),
        hovered = ButtonColors(containerColor = Color(0xFFF56C6C), contentColor = Color.White, borderColor = Color(0xFFF56C6C)),
        pressed = ButtonColors(containerColor = Color(0xFFD9534F), contentColor = Color.White, borderColor = Color(0xFFD9534F))
    )

}
