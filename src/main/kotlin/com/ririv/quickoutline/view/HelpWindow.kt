package com.ririv.quickoutline.view

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.net.URI

@Composable
fun HelpWindow() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Version: 2.2.0")
        Row {
            Text("Usage: ")
            Text(
                text = "@Github",
                color = Color.Blue,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures {
                        Desktop.getDesktop().browse(URI("https://github.com/ririv/QuickOutline"))
                    }
                }
            )
        }
        Text("Dependencies: iText (AGPL license)")
        Row {
            Text("Source: ")
            Text(
                text = "GitHub",
                color = Color.Blue,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures {
                        Desktop.getDesktop().browse(URI("https://github.com/ririv/QuickOutline"))
                    }
                }
            )
        }
        Row {
            Text("By: ")
            Text(
                text = "Ririv",
                color = Color.Blue,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures {
                        Desktop.getDesktop().browse(URI("https://www.xiaohongshu.com/user/profile/5f988414000000000101ca29"))
                    }
                }
            )
            Text(" G") // Github icon placeholder
            Text(" X") // Xiaohongshu icon placeholder
        }
    }
}
