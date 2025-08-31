package com.ririv.quickoutline.view

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import org.koin.java.KoinJavaComponent.inject

@Composable
fun TocGeneratorTab() {
    val viewModel: TocGeneratorViewModel by inject(TocGeneratorViewModel::class.java)

    Column {
        Button(onClick = {
            viewModel.generateToc()
        }) {
            Text("Generate TOC")
        }
        TextField(
            value = viewModel.generatedToc,
            onValueChange = { viewModel.generatedToc = it },
            label = { Text("Generated TOC") }
        )
    }
}
