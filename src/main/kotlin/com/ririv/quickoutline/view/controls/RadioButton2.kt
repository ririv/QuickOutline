package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RadioButton2Group(items: List<String>, selectedItem: String, onItemSelected: (String) -> Unit) {
    Row {
        items.forEachIndexed { index, item ->
            val shape = when (index) {
                0 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                items.size - 1 -> RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
                else -> RoundedCornerShape(0.dp)
            }

            OutlinedButton(
                onClick = { onItemSelected(item) },
                shape = shape,
                border = BorderStroke(1.dp, Color(0xFF409EFF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (selectedItem == item) Color(0xFF409EFF) else Color.Transparent,
                    contentColor = if (selectedItem == item) Color.White else Color(0xFF409EFF)
                ),
                modifier = if (index > 0) Modifier.offset(x = (-1).dp) else Modifier
            ) {
                Text(item)
            }
        }
    }
}
