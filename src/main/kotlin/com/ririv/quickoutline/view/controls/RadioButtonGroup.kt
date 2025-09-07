package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
Usage

@Composable
fun YourComponent() {
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
            println("用户选择了: $newValue")
        }
    )
}
*/

@Composable
fun <T> RadioButtonGroup(
    items: List<Pair<String, T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(0xFF409EFF)
    val onPrimaryColor = Color.White

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min) // Constrain height to the minimum intrinsic height of children
            .clip(RoundedCornerShape(6.dp)) // Clip the children to the rounded shape
            .border(1.dp, primaryColor, RoundedCornerShape(6.dp))
    ) {
        items.forEachIndexed { index, (text, value) ->
            val isSelected = selectedItem == value
            Box(
                modifier = Modifier
                    .background(if (isSelected) primaryColor else Color.Transparent)
                    .clickable { onItemSelected(value) }
                    .padding(horizontal = 16.dp, vertical = 8.dp), // Apply padding here
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = if (isSelected) onPrimaryColor else primaryColor
                )
            }

            if (index < items.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxHeight().width(1.dp),
                    color = primaryColor
                )
            }
        }
    }
}