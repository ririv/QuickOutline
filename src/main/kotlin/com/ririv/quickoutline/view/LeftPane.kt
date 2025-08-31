package com.ririv.quickoutline.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import com.ririv.quickoutline.view.controls.ButtonType
import com.ririv.quickoutline.view.controls.StyledButton

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
        StyledButton(
            onClick = { onTabSelected(0) },
            text = stringResource("leftPane.bookmarkTabBtn"),
            type = if (selectedTab == 0) ButtonType.PLAIN_PRIMARY else ButtonType.DEFAULT
        )
        StyledButton(
            onClick = { onTabSelected(2) },
            text = stringResource("leftPane.labelTabBtn"),
            type = if (selectedTab == 2) ButtonType.PLAIN_PRIMARY else ButtonType.DEFAULT
        )
        StyledButton(
            onClick = { onTabSelected(1) },
            text = stringResource("leftPane.tocGeneratorTabBtn"),
            type = if (selectedTab == 1) ButtonType.PLAIN_PRIMARY else ButtonType.DEFAULT
        )
        StyledButton(
            onClick = { onTabSelected(3) },
            text = stringResource("leftPane.previewTabBtn"),
            type = if (selectedTab == 3) ButtonType.PLAIN_PRIMARY else ButtonType.DEFAULT
        )

        Spacer(modifier = Modifier.weight(1f))

        StyledButton(onClick = { /* TODO: Implement settings */ }, text = stringResource("leftPane.settingBtn"))
        StyledButton(onClick = { showHelpWindow = true }, text = stringResource("leftPane.helpBtn"))
    }

    if (showHelpWindow) {
        Window(onCloseRequest = { showHelpWindow = false }, title = stringResource("helpWindow.title")) {
            HelpWindow()
        }
    }
}
