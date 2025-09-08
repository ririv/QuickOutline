package com.ririv.quickoutline.view.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.icons.AppIcon
import com.ririv.quickoutline.view.icons.AppIcon as IconEnum
import com.ririv.quickoutline.view.icons.AppIcon as Icon
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState

@Composable
fun LeftPane(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    var showHelpWindow by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(Color(0xFFF8F8F8))
            .border(width = 1.dp, color = Color(0xFFDFDFDF))
            .padding(8.dp)
    ) {
        IconToggleButton(
            checked = selectedTab == 0,
            onCheckedChange = { onTabSelected(0) }
        ) {
            com.ririv.quickoutline.view.icons.AppIcon(icon = AppIcon.Bookmark, contentDescription = "Bookmark", modifier = Modifier.size(24.dp), tint = if (selectedTab == 0) Color(0xFF409EFF) else Color.Gray)
        }
        IconToggleButton(
            checked = selectedTab == 2,
            onCheckedChange = { onTabSelected(2) }
        ) {
            com.ririv.quickoutline.view.icons.AppIcon(icon = AppIcon.PageLabelSingle, contentDescription = "Label", modifier = Modifier.size(24.dp), tint = if (selectedTab == 2) Color(0xFF409EFF) else Color.Gray)
        }
        IconToggleButton(
            checked = selectedTab == 1,
            onCheckedChange = { onTabSelected(1) }
        ) {
            com.ririv.quickoutline.view.icons.AppIcon(icon = AppIcon.Toc, contentDescription = "TOC", modifier = Modifier.size(24.dp), tint = if (selectedTab == 1) Color(0xFF409EFF) else Color.Gray)
        }
        IconToggleButton(
            checked = selectedTab == 3,
            onCheckedChange = { onTabSelected(3) }
        ) {
            com.ririv.quickoutline.view.icons.AppIcon(icon = AppIcon.FeatureLandscape, contentDescription = "Preview", modifier = Modifier.size(24.dp), tint = if (selectedTab == 3) Color(0xFF409EFF) else Color.Gray)
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = { /* TODO: Implement settings */ }) {
            com.ririv.quickoutline.view.icons.AppIcon(icon = AppIcon.Setting, contentDescription = "Settings", modifier = Modifier.size(24.dp), tint = Color.Gray)
        }
        IconButton(onClick = { showHelpWindow = true }) {
            com.ririv.quickoutline.view.icons.AppIcon(icon = AppIcon.Help, contentDescription = "Help", modifier = Modifier.size(24.dp), tint = Color.Gray)
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