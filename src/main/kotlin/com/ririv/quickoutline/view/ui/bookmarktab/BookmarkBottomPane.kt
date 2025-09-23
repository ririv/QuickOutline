package com.ririv.quickoutline.view.ui.bookmarktab

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ririv.quickoutline.pdfProcess.ViewScaleType
import com.ririv.quickoutline.view.icons.AppIcon
import com.ririv.quickoutline.view.ui.stringResource
import com.ririv.quickoutline.view.viewmodel.BookmarkViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.ririv.quickoutline.view.controls.*
import com.ririv.quickoutline.view.controls.GraphIconToggleButton

@Composable
private fun SetContentsPopupContent(onSelect: (ViewScaleType) -> Unit) {
    var selected by remember { mutableStateOf(ViewScaleType.NONE) }

    val viewScaleText = when (selected) {
        ViewScaleType.FIT_TO_HEIGHT -> stringResource("viewScaleTypeLabel.FIT_TO_HEIGHT")
        ViewScaleType.FIT_TO_WIDTH -> stringResource("viewScaleTypeLabel.FIT_TO_WIDTH")
        ViewScaleType.ACTUAL_SIZE -> stringResource("viewScaleTypeLabel.ACTUAL_SIZE")
        else -> stringResource("viewScaleTypeLabel.NONE")
    }

    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
//        Box(
////            contentAlignment = Alignment.Start
//        ) {
            Text(
                stringResource("setContentsPopup.title"),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9198A1),
                textAlign = TextAlign.Left,
                modifier=Modifier.align(Alignment.Start)
            )
//        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modifier = Modifier.clip(RoundedCornerShape(5.dp)).size(22.dp)
            GraphIconToggleButton(
                checked = selected == ViewScaleType.FIT_TO_HEIGHT,
                onCheckedChange = {
                    val newSelection = if (it) ViewScaleType.FIT_TO_HEIGHT else ViewScaleType.NONE
                    selected = newSelection
                    onSelect(newSelection)
                },
                modifier
            ) {
                AppIcon(icon = AppIcon.FitToHeight, modifier = Modifier.size(24.dp), tint = Color.Gray)
            }
            GraphIconToggleButton(
                checked = selected == ViewScaleType.FIT_TO_WIDTH,
                onCheckedChange = {
                    val newSelection = if (it) ViewScaleType.FIT_TO_WIDTH else ViewScaleType.NONE
                    selected = newSelection
                    onSelect(newSelection)
                },
                modifier
            ) {
                AppIcon(icon = AppIcon.FitToWidth, modifier = Modifier.size(24.dp), tint = Color.Gray)
            }
            GraphIconToggleButton(
                checked = selected == ViewScaleType.ACTUAL_SIZE,
                onCheckedChange = {
                    val newSelection = if (it) ViewScaleType.ACTUAL_SIZE else ViewScaleType.NONE
                    selected = newSelection
                    onSelect(newSelection)
                },
                modifier
            ) {
                AppIcon(icon = AppIcon.ActualSize, modifier = Modifier.size(24.dp), tint = Color.Gray)
            }
        }
        Text(viewScaleText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GetContentsPopupContent(onSelect: (String) -> Unit) {
    var selected by remember { mutableStateOf("bookmark") }
    Column(modifier = Modifier.padding(8.dp)) {
        Text("获取目录",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9198A1),
            textAlign = TextAlign.Left,
            modifier=Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))

        // 1. 定义你的选项列表
        val options = listOf(
            "书签" to "bookmark",
            "目录" to "toc"
        )

        // 2. 创建一个状态来保存当前选中的项目，默认选中第一个
        var selectedValue by remember { mutableStateOf("bookmark") }

        // 3. 调用 RadioButtonGroup 组件
        RadioButtonGroup(
            items = options,
            selectedItem = selectedValue,
            onItemSelected = { newValue ->
                // 当用户点击时，更新状态
                selectedValue = newValue
            }
        )
    }
}

@Composable
fun BookmarkBottomPane(viewModel: BookmarkViewModel, showTreeView: Boolean, onSwitchView: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var getContentsSelection by remember { mutableStateOf("bookmark") }
    var setContentsSelection by remember { mutableStateOf(ViewScaleType.NONE) }

    Row(
        modifier = Modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val deleteInteractionSource = remember { MutableInteractionSource() }
        val isDeleteHovered by deleteInteractionSource.collectIsHoveredAsState()
        val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
        val targetDeleteIconTint = when {
            isDeletePressed -> Color(0xFFC45656)
            isDeleteHovered -> Color(0xFFf56c6c)
            else -> Color.Gray
        }
        val deleteIconTint by animateColorAsState(targetDeleteIconTint)

        val switchInteractionSource = remember { MutableInteractionSource() }
        val isSwitchHovered by switchInteractionSource.collectIsHoveredAsState()
        val isSwitchPressed by switchInteractionSource.collectIsPressedAsState()
        val targetSwitchIconTint = when {
            isSwitchPressed -> Color(0xE5969696)
            isSwitchHovered -> Color(0xFF363636)
            else -> Color.Gray
        }
        val switchIconTint by animateColorAsState(targetSwitchIconTint)

        Box(
            modifier = Modifier.clickable(
                onClick = { viewModel.deleteBookmark() },
                indication = null,
                interactionSource = deleteInteractionSource
            )
        ) {
            AppIcon(icon = AppIcon.Delete, contentDescription = "Delete", modifier = Modifier.size(24.dp), tint = deleteIconTint)
        }

        Box(modifier = Modifier.weight(1f)) {
            PopupCard(
                popupContent = {
                    GetContentsPopupContent { getContentsSelection = it }
                },
                triggers = setOf(PopupTriggerType.INSTANT_ON_HOVER),
                position = PopupPosition.TOP_CENTER,
                modifier = Modifier.align(Alignment.Center)
            ) { modifier ->
                StyledButton(
                    onClick = {
                        if (getContentsSelection == "bookmark") {
                            viewModel.loadBookmarks()
                        } else {
                            viewModel.extractToc()
                        }
                    },
                    text = stringResource("bookmarkTab.getContentsBtn.text"),
                    type = ButtonType.PLAIN_PRIMARY,
                    modifier = modifier.width(120.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            PopupCard(
                popupContent = {
                    SetContentsPopupContent { setContentsSelection = it }
                },
                triggers = setOf(PopupTriggerType.INSTANT_ON_HOVER),
                position = PopupPosition.TOP_CENTER,
                modifier = Modifier.align(Alignment.Center)
            ) { modifier ->
                StyledButton(
                    onClick = { viewModel.saveBookmarks(setContentsSelection) },
                    text = stringResource("bookmarkTab.setContentsBtn.text"),
                    type = ButtonType.PLAIN_IMPORTANT,
                    modifier = modifier.width(120.dp)
                )
            }
        }

        StyledTextField(
            value = uiState.offset,
            onValueChange = { viewModel.setOffset(it) },
//            label = { Text(stringResource("bookmarkTab.offsetTF.prompt")) },
            modifier = Modifier.weight(1f),
//            colors = TextFieldDefaults.colors(
//                focusedContainerColor = Color.Transparent,
//                unfocusedContainerColor = Color.Transparent,
//            ),
            placeholder = { Text(stringResource("bookmarkTab.offsetTF.prompt")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Box(
            modifier = Modifier.clickable(
                onClick = onSwitchView,
                indication = null,
                interactionSource = switchInteractionSource
            )
        ) {
            AppIcon(icon = if (showTreeView) AppIcon.TextEdit else AppIcon.TreeDiagram, contentDescription = "Switch view", modifier = Modifier.size(24.dp), tint = switchIconTint)
        }
    }
}
