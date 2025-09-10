package com.ririv.quickoutline.view.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.icons.AppIcon
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.ririv.quickoutline.view.ui.controls.GraphIconButton
import com.ririv.quickoutline.view.ui.controls.GraphIconToggleButton

@Composable
fun LeftPane(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    var showHelpWindow by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(Color(0xFFF8F8F8))
            .border(width = 1.dp, color = Color(0xFFDFDFDF))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // Add vertical spacing
    ) {
        GraphIconToggleButton(
            checked = selectedTab == 0,
            onCheckedChange = { onTabSelected(0) },
            modifier = Modifier.size(40.dp)
        ) {
            AppIcon(icon = AppIcon.Bookmark, contentDescription = "Bookmark", modifier = Modifier.size(24.dp), tint = if (selectedTab == 0) Color(0xFF409EFF) else Color.Gray)
        }
        GraphIconToggleButton(
            checked = selectedTab == 2,
            onCheckedChange = { onTabSelected(2) },
            modifier = Modifier.size(40.dp)
        ) {
            AppIcon(icon = AppIcon.PageLabelSingle, contentDescription = "Label", modifier = Modifier.size(24.dp), tint = if (selectedTab == 2) Color(0xFF409EFF) else Color.Gray)
        }
        GraphIconToggleButton(
            checked = selectedTab == 1,
            onCheckedChange = { onTabSelected(1) },
            modifier = Modifier.size(40.dp)
        ) {
            AppIcon(icon = AppIcon.Toc, contentDescription = "TOC", modifier = Modifier.size(24.dp), tint = if (selectedTab == 1) Color(0xFF409EFF) else Color.Gray)
        }
        GraphIconToggleButton(
            checked = selectedTab == 3,
            onCheckedChange = { onTabSelected(3) },
            modifier = Modifier.size(40.dp)
        ) {
            AppIcon(icon = AppIcon.FeatureLandscape, contentDescription = "Preview", modifier = Modifier.size(24.dp), tint = if (selectedTab == 3) Color(0xFF409EFF) else Color.Gray)
        }

        Spacer(modifier = Modifier.weight(1f))

        GraphIconButton(onClick = { /* TODO: Implement settings */ }, modifier = Modifier.size(40.dp)) {
            AppIcon(icon = AppIcon.Setting, contentDescription = "Settings", modifier = Modifier.size(24.dp), tint = Color.Gray)
        }
        GraphIconButton(onClick = { showHelpWindow = true }, modifier = Modifier.size(40.dp)) {
            AppIcon(icon = AppIcon.Help, contentDescription = "Help", modifier = Modifier.size(24.dp), tint = Color.Gray)
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