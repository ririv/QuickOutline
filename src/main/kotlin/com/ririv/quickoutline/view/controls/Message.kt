package com.ririv.quickoutline.view.controls

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class MessageType {
    SUCCESS, INFO, WARNING, ERROR
}

@Composable
fun Message(text: String, type: MessageType, modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    val backgroundColor = when (type) {
        MessageType.SUCCESS -> Color(0xFFF0F9EB)
        MessageType.INFO -> Color(0xFFF4F4F5)
        MessageType.WARNING -> Color(0xFFFDF6EC)
        MessageType.ERROR -> Color(0xFFFEF0F0)
    }

    val contentColor = when (type) {
        MessageType.SUCCESS -> Color(0xFF67C23A)
        MessageType.INFO -> Color(0xFF909399)
        MessageType.WARNING -> Color(0xFFE6A23C)
        MessageType.ERROR -> Color(0xFFF56C6C)
    }

    val iconText = when (type) {
        MessageType.SUCCESS -> "✓"
        MessageType.INFO -> "ⓘ"
        MessageType.WARNING -> "⚠"
        MessageType.ERROR -> "✗"
    }

    val visible = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000)
        visible.value = false
        delay(500) // wait for fade out animation to finish
        onDismiss()
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible.value,
        enter = fadeIn(animationSpec = tween(500)),
        exit = fadeOut(animationSpec = tween(500)),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.padding(8.dp),
            shape = RoundedCornerShape(6.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = backgroundColor,
                contentColor = contentColor,
            ),
//            elevation = 4.dp // Set elevation to 0.dp to remove shadow
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = iconText, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}