package com.ririv.quickoutline.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.view.controls.ButtonType
import com.ririv.quickoutline.view.controls.StyledButton
import com.ririv.quickoutline.view.controls.StyledTextField
import com.ririv.quickoutline.view.viewmodel.BookmarkViewModel

@Composable
fun BookmarkBottomPane(viewModel: BookmarkViewModel, showTreeView: Boolean, onSwitchView: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Row(
        modifier = Modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val deleteInteractionSource = remember { MutableInteractionSource() }
        val isDeleteHovered by deleteInteractionSource.collectIsHoveredAsState()
        val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
        val deleteIconTint = when {
            isDeletePressed -> Color(0xFFC45656)
            isDeleteHovered -> Color(0xFFf56c6c)
            else -> Color.Gray
        }

        val switchInteractionSource = remember { MutableInteractionSource() }
        val isSwitchHovered by switchInteractionSource.collectIsHoveredAsState()
        val isSwitchPressed by switchInteractionSource.collectIsPressedAsState()
        val switchIconTint = when {
            isSwitchPressed -> Color(0xE5969696)
            isSwitchHovered -> Color(0xFF363636)
            else -> Color.Gray
        }

        Box(
            modifier = Modifier.clickable(
                onClick = { viewModel.deleteBookmark() },
                indication = null,
                interactionSource = deleteInteractionSource
            )
        ) {
            Icon(
                painter = loadResourcePainter("drawable/delete.svg"),
                contentDescription = "Delete",
                modifier = Modifier.size(24.dp),
                tint = deleteIconTint
            )
        }

        StyledButton(
            onClick = { viewModel.loadBookmarks() },
            text = stringResource("bookmarkTab.getContentsBtn.text"),
            type = ButtonType.PLAIN_PRIMARY,
            modifier = Modifier.weight(1f)
        )
        StyledButton(
            onClick = { viewModel.saveBookmarks() },
            text = stringResource("bookmarkTab.setContentsBtn.text"),
            type = ButtonType.PLAIN_IMPORTANT,
            modifier = Modifier.weight(1f)
        )
        StyledTextField(
            value = uiState.offset,
            onValueChange = { viewModel.setOffset(it) },
            placeholder = { Text(stringResource("bookmarkTab.offsetTF.prompt")) },
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier.clickable(
                onClick = onSwitchView,
                indication = null,
                interactionSource = switchInteractionSource
            )
        ) {
            Icon(
                painter = loadResourcePainter(if (showTreeView) "drawable/text-edit.svg" else "drawable/tree-diagram.svg"),
                contentDescription = "Switch view",
                modifier = Modifier.size(24.dp),
                tint = switchIconTint
            )
        }
    }
}