package com.ririv.quickoutline.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.java.KoinJavaComponent.inject

@Composable
fun PageLabelTabView() {
    val viewModel: PageLabelViewModel by inject(PageLabelViewModel::class.java)

    TextField(
        value = viewModel.pageLabels,
        onValueChange = { viewModel.pageLabels = it },
        modifier = Modifier.fillMaxSize()
    )
}
