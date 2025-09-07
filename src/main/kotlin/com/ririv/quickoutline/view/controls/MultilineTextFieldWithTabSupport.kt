package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MultilineTextFieldWithTabSupport(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val vScrollState = rememberScrollState()
    val hScrollState = rememberScrollState()

    val borderColor = when {
        isFocused -> Color(0xFF409EFF)
        isHovered -> Color(0xFF409EFF)
        else -> Color.Transparent
    }

    Box(modifier = modifier.border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(4.dp))) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp, bottom = 12.dp) // Prevent text from overlapping with the scrollbars
                .verticalScroll(vScrollState)
                .horizontalScroll(hScrollState)
                .hoverable(interactionSource = interactionSource)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Tab) {
                        val newValue = if (keyEvent.isShiftPressed) {
                            removeIndent(value)
                        } else {
                            addIndent(value)
                        }
                        onValueChange(newValue)
                        true
                    } else {
                        false
                    }
                },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            interactionSource = interactionSource,
            placeholder = placeholder,
            singleLine = false,
            enabled = enabled
        )

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(all = 4.dp),
            adapter = rememberScrollbarAdapter(vScrollState)
        )

        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(all = 4.dp),
            adapter = rememberScrollbarAdapter(hScrollState)
        )
    }
}

// 用于匹配行首缩进的正则表达式，可以是1个Tab或1-4个空格
private val INDENT_PATTERN: Pattern = Pattern.compile("^(\\t|\\s{1,4})")

/**
 * 处理增加缩进的逻辑 (按下 Tab)
 */
private fun addIndent(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val selection = value.selection
    val tab = "\t"

    // 情况1：没有选择文本，只有一个光标。直接在光标处插入一个Tab。
    if (selection.collapsed) {
        val newText = text.replaceRange(selection.start, selection.end, tab)
        return TextFieldValue(newText, TextRange(selection.start + 1))
    }

    val start = min(selection.start, selection.end)
    val end = max(selection.start, selection.end)

    // 判断选区是否跨越多行
    val isMultiLine = text.substring(start, end).contains('\n')

    if (isMultiLine) {
        // 情况2：选中了多行。
        // 计算选区覆盖的起始行和结束行。
        val startLine = text.substring(0, start).count { it == '\n' }
        val endLine = text.substring(0, end).count { it == '\n' }

        val lines = text.split('\n').toMutableList()
        var charsAdded = 0
        // 为选区内的每一行（非空行）前面添加一个Tab。
        for (i in startLine..endLine) {
            if (i < lines.size && lines[i].isNotEmpty()) {
                lines[i] = tab + lines[i]
                charsAdded++
            }
        }

        val newText = lines.joinToString("\n")
        // 更新选区，确保新添加的Tab也被包含在内。
        return TextFieldValue(newText, TextRange(start, end + charsAdded))
    } else {
        // 情况3：只选中了单行内的部分或全部内容。
        val lineStart = text.lastIndexOf('\n', start - 1) + 1
        val lineEnd = text.indexOf('\n', start).let { if (it == -1) text.length else it }

        // 判断是否选中了整行（从行首到行尾，可以包含或不包含最后的换行符）。
        val isFullLineSelected = start == lineStart && (end == lineEnd || (end == lineEnd + 1 && text.getOrNull(lineEnd) == '\n'))

        if (isFullLineSelected) {
            // 3a：如果选中了整行，则在行首添加缩进。
            val newText = text.replaceRange(lineStart, lineStart, tab)
            // 更新选区，确保新添加的Tab也被包含在内。
            return TextFieldValue(newText, TextRange(start, end + 1))
        } else {
            // 3b：如果只选中了行内的一部分，则用Tab替换选中的内容。
            val newText = text.replaceRange(start, end, tab)
            return TextFieldValue(newText, TextRange(start + 1))
        }
    }
}

/**
 * 处理移除缩进的逻辑 (按下 Shift+Tab)
 */
private fun removeIndent(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val selection = value.selection

    var start = min(selection.start, selection.end)
    var end = max(selection.start, selection.end)

    // 关键修复：如果选区的起点恰好是一个换行符，则逻辑上将起点后移一位。
    // 这是为了防止当光标从上一行末尾开始选择时，错误地修改了上一行的内容。
    if (start > 0 && start < text.length && text[start - 1] == '\n') {
        // 此情况由下面的基于行的逻辑统一处理
    } else if (selection.collapsed) {
        // 情况1：没有选择文本，只有一个光标。
        val caretPosition = selection.start
        if (caretPosition == 0) return value

        val lineStart = text.lastIndexOf('\n', caretPosition - 1) + 1
        val line = text.substring(lineStart)

        // 检查当前行是否有缩进，如果有则移除。
        val matcher = INDENT_PATTERN.matcher(line)
        if (matcher.find()) {
            val indent = matcher.group(0) ?: ""
            val len = indent.length
            if (lineStart + len <= text.length) {
                val newText = text.removeRange(lineStart, lineStart + len)
                // 更新光标位置。
                return TextFieldValue(newText, TextRange((caretPosition - len).coerceAtLeast(lineStart)))
            }
        }
        return value // 没有缩进可移除
    }

    // 修正多行选择的起始点，如果选区以换行符开始，则忽略这个换行符。
    if (start < text.length && text[start] == '\n') {
        start++
    }
    if (start >= end) return value

    // 计算选区覆盖的起始行和结束行。
    val startLineNum = text.substring(0, start).count { it == '\n' }
    // 如果选区末尾正好是换行符，则不应包含下一行。
    val effectiveEnd = if (end > 0 && text[end - 1] == '\n') end - 1 else end
    val endLineNum = text.substring(0, effectiveEnd).count { it == '\n' }

    val lines = text.split('\n').toMutableList()
    var charsRemoved = 0
    var firstLineCharsRemoved = 0

    // 为选区内的每一行移除一个缩进单位。
    for (i in startLineNum..endLineNum) {
        if (i < lines.size) {
            val matcher = INDENT_PATTERN.matcher(lines[i])
            if (matcher.find()) {
                val indent = matcher.group(0) ?: ""
                val len = indent.length
                lines[i] = lines[i].substring(len)
                charsRemoved += len
                if (i == startLineNum) {
                    firstLineCharsRemoved = len
                }
            }
        }
    }

    val newText = lines.joinToString("\n")

    // 更新选区。
    val newStart = (start - firstLineCharsRemoved).coerceAtLeast(0)
    val newEnd = (end - charsRemoved).coerceAtLeast(0)

    return TextFieldValue(
        newText,
        TextRange(newStart, newEnd)
    )
}