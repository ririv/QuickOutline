package com.ririv.quickoutline.view

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.utils.InfoUtil
import java.awt.Desktop
import java.net.URI

@Composable
fun HelpWindow() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Version
        Text("Version: ${InfoUtil.getAppVersion()}")

        // Usage
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource("helpWindow.usage"))
            HyperlinkText(
                text = "@Github",
                onClick = { browse("https://github.com/ririv/QuickOutline/blob/master/README.md") }
            )
        }

        // Dependencies
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource("helpWindow.dependenciesText"))
            Text(": iText (AGPL license)")
        }

        // Source
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = loadResourcePainter("drawable/github.svg"),
                contentDescription = "GitHub",
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
            Spacer(Modifier.width(4.dp))
            HyperlinkText(
                text = "Source",
                onClick = { browse("https://github.com/ririv/QuickOutline") }
            )
        }

        // Author
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("By ")
            HyperlinkText(
                text = stringResource("helpWindow.author"),
                onClick = { browse("https://www.xiaohongshu.com/user/profile/5f988414000000000101ca29") }
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = loadResourcePainter("drawable/xiaohongshu.svg"),
                contentDescription = "Xiaohongshu",
                modifier = Modifier.size(16.dp),
                tint = Color(0xFFADADAD)
            )
        }
    }
}

@Composable
private fun HyperlinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.Blue,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures {
                onClick()
            }
        }
    )
}

private fun browse(url: String) {
    try {
        Desktop.getDesktop().browse(URI(url))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}