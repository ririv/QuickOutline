package com.ririv.quickoutline.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LeftPane(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxHeight().padding(8.dp)) {
        IconToggleButton(checked = selectedTab == 0, onCheckedChange = { onTabSelected(0) }) {
            Text("B")
        }
        IconToggleButton(checked = selectedTab == 1, onCheckedChange = { onTabSelected(1) }) {
            Text("L")
        }
        IconToggleButton(checked = selectedTab == 2, onCheckedChange = { onTabSelected(2) }) {
            Text("T")
        }
        IconToggleButton(checked = selectedTab == 3, onCheckedChange = { onTabSelected(3) }) {
            Text("P")
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = { /* TODO: Implement settings */ }) {
            Text("S")
        }
        IconButton(onClick = { /* TODO: Implement help */ }) {
            Text("H")
        }
    }
}
