package com.ririv.quickoutline.view.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.controls.ButtonType
import com.ririv.quickoutline.view.controls.StyledButton
import com.ririv.quickoutline.view.controls.StyledTextField
import com.ririv.quickoutline.view.viewmodel.TocGeneratorViewModel
import org.koin.java.KoinJavaComponent.inject

@Composable
fun TocGeneratorTab() {
    val viewModel: TocGeneratorViewModel by inject(TocGeneratorViewModel::class.java)

    Column(modifier = Modifier.padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyledButton(
                onClick = { viewModel.generateToc() },
                text = "Generate TOC",
                type = ButtonType.PLAIN_PRIMARY
            )
            if (viewModel.isGenerating) {
                Text("Generating...")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(viewModel.status)
        Spacer(modifier = Modifier.height(8.dp))

        var tfv by remember(viewModel.generatedToc) { mutableStateOf(TextFieldValue(viewModel.generatedToc)) }
        StyledTextField(
            value = tfv,
            onValueChange = { 
                tfv = it
                viewModel.generatedToc = it.text
             },
            placeholder = { Text("Generated TOC") },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}