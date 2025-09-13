package com.ririv.quickoutline.view.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.ririv.quickoutline.view.icons.AppIcon
import com.ririv.quickoutline.view.controls.GraphIconButton
import com.ririv.quickoutline.view.controls.GraphIconToggleButton

@Composable
fun LeftPane(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    var showHelpWindow by remember { mutableStateOf(false) }

    val graphButtonModifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
    val iconModifier = Modifier.size(20.dp)
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(50.dp)
            .background(Color(0xFFF8F8F8))
            .drawWithContent {
                drawContent()
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = Color(0xFFDFDFDF),
                    start = Offset(size.width - strokeWidth / 2, 0f),
                    end = Offset(size.width - strokeWidth / 2, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // Add vertical spacing
    ) {

        GraphIconToggleButton(
            checked = selectedTab == 0,
            onCheckedChange = { onTabSelected(0) },
            modifier = graphButtonModifier
        ) {
            AppIcon(
                icon = AppIcon.Bookmark,
                contentDescription = "Bookmark",
                modifier = iconModifier,
                tint = if (selectedTab == 0) Color(0xFF409EFF) else Color.Gray
            )
        }
        GraphIconToggleButton(
            checked = selectedTab == 2,
            onCheckedChange = { onTabSelected(2) },
            modifier = graphButtonModifier
        ) {
            AppIcon(
                icon = AppIcon.PageLabelSingle,
                contentDescription = "Label",
                modifier = iconModifier,
                tint = if (selectedTab == 2) Color(0xFF409EFF) else Color.Gray
            )
        }
        GraphIconToggleButton(
            checked = selectedTab == 1,
            onCheckedChange = { onTabSelected(1) },
            modifier = graphButtonModifier
        ) {
            AppIcon(icon = AppIcon.Toc, contentDescription = "TOC", modifier = iconModifier, tint = if (selectedTab == 1) Color(0xFF409EFF) else Color.Gray)
        }
        GraphIconToggleButton(
            checked = selectedTab == 3,
            onCheckedChange = { onTabSelected(3) },
            modifier = graphButtonModifier
        ) {
            AppIcon(
                icon = AppIcon.FeatureLandscape,
                contentDescription = "Preview",
                modifier = iconModifier,
                tint = if (selectedTab == 3) Color(0xFF409EFF) else Color.Gray
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        GraphIconButton(onClick = { /* TODO: Implement settings */ }, modifier = graphButtonModifier) {
            AppIcon(icon = AppIcon.Setting, contentDescription = "Settings", modifier = iconModifier, tint = Color.Gray)
        }
        GraphIconButton(onClick = { showHelpWindow = true }, modifier = graphButtonModifier) {
            AppIcon(icon = AppIcon.Help, contentDescription = "Help", modifier = iconModifier, tint = Color.Gray)
        }
    }

    if (showHelpWindow) {
        Window(
            onCloseRequest = { showHelpWindow = false },
            title = "Help",
            state = rememberWindowState(width = 400.dp, height = 300.dp)
        ) {
            HelpWindow()
        }
    }
}