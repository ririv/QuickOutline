package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun <T> RadioButton2Group(
    items: List<Pair<String, T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        items.forEachIndexed { index, (text, value) ->
            val shape = when (index) {
                0 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                items.size - 1 -> RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
                else -> RoundedCornerShape(0.dp)
            }

            OutlinedButton(
                onClick = { onItemSelected(value) },
                shape = shape,
                border = BorderStroke(1.dp, Color(0xFF409EFF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selectedItem == value) Color(0xFF409EFF) else Color.Transparent,
                    contentColor = if (selectedItem == value) Color.White else Color(0xFF409EFF)
                ),
                modifier = if (index > 0) Modifier.offset(x = (-1).dp) else Modifier
            ) {
                Text(text)
            }
        }
    }
}