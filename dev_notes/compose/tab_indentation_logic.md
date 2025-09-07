# Compose 多行文本框 Tab 缩进功能实现详解

**日期:** 2025年9月7日

## 1. 引言

在开发富文本编辑器或代码编辑器时，一个基础且重要的功能是通过 `Tab` 键来增加代码缩进，以及通过 `Shift+Tab` 来减少缩进。这个功能需要处理各种复杂的边界情况，例如：没有文本选中、选中单行或多行、选中部分或整行等。本文档详细记录了在 Jetpack Compose 中，为一个多行 `TextField` 实现与 VS Code 编辑器行为一致的 Tab 缩进功能的全过程，包括最终的实现方案、遇到的挑战以及解决这些问题的迭代思路。

## 2. 核心逻辑与最终实现

我们的目标是模拟 VS Code 的缩进逻辑，其核心行为可以总结如下：

- **增加缩进 (Tab)**
  - **无选区**：在光标处插入一个 Tab 字符。
  - **单行内部分选区**：用一个 Tab 字符替换选中的文本。
  - **单行整行选区**：在该行行首添加一级缩进。
  - **多行选区**：在所有被选中的行首各添加一级缩进。

- **减少缩进 (Shift+Tab)**
  - **无选区**：移除光标所在行的行首一级缩进。
  - **有选区**：移除所有被选中行（无论部分或整行）的行首一级缩进。

为了实现这一功能，我们为 Compose 的 `TextField` 添加了 `.onPreviewKeyEvent` 修饰符，以确保我们的自定义逻辑能在组件默认行为之前被触发和消费，防止冲突。

### 最终代码实现

以下是控制缩进逻辑的核心代码，包含了详尽的注释来解释各种情况的处理。

```kotlin
// 用于匹配行首缩进的正则表达式，可以是1个Tab或1-4个空格
private val INDENT_PATTERN: Pattern = Pattern.compile("^(\t|\s{1,4})")

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
    val end = max(selection.start, selection.end)

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
```

## 3. 遇到的问题与迭代过程

功能的实现并非一帆风顺，我们经历了几次迭代才达到最终的理想效果。这个过程暴露了在 Compose 中处理文本输入和事件的几个关键要点。

### 3.1. 初始尝试：直接翻译 JavaFX 逻辑

我们最初的思路是直接将之前在 JavaFX 中实现的缩进逻辑翻译到 Compose。然而，这种命令式的、直接操作 UI 组件状态的方式与 Compose 的声明式、状态驱动的理念格格不入，导致了许多问题，最严重的是 `StringIndexOutOfBoundsException` 异常，原因是选区 `TextRange` 在连续操作后出现了 `start` 大于 `end` 的非法状态。

### 3.2. 关键转折：`onKeyEvent` vs `onPreviewKeyEvent`

我们遇到的一个核心问题是：在多行选择文本后按 Tab，选中的文本会被直接替换成一个 Tab 字符，而不是我们期望的缩进行为。这表明 `TextField` 的默认 Tab 行为（替换选区）在我们的逻辑之前执行了。

**解决方案**：将事件处理器从 `Modifier.onKeyEvent` 更换为 `Modifier.onPreviewKeyEvent`。后者在事件向下传递（Preview 阶段）时触发，允许我们先于 `TextField` 的内置处理器捕获并消费掉 Tab 事件，从而确保了我们自定义逻辑的优先执行。

### 3.3. Shift+Tab 的“幽灵”Bug

在修复了 Tab 的问题后，Shift+Tab 出现了两个新问题：
1.  在单行上（无选区）按键无效。
2.  连续两次对多行进行操作时，会错误地影响到选区上一行的内容。

通过您的细致观察，我们发现这个 bug 与 JavaFX 早期版本中的一个已知问题非常相似。根源在于对**选区起点的判断**。当选区从上一行的末尾（即换行符 `
` 之后）开始时，我们的代码错误地将上一行也计算在内。通过增加一个简单的判断，如果选区以 `
` 开始，则将逻辑起点向后移动一位，我们成功修复了这个问题。

### 3.4. Tab 键的最后一块拼图：整行选择

最后，我们处理了 Tab 键在单行选择时的最后一个不一致行为。当用户选中一整行时，正确的行为应该是缩进该行，而不是替换它。

**解决方案**：在处理单行选择的逻辑中，增加一个对“是否为整行选择”的判断。通过检查选区的 `start` 和 `end` 是否与该行的起止位置完全对应，我们可以精确地区分出“部分选择”和“整行选择”，并分别执行“替换”或“缩进”操作。同时，在缩进后，我们必须正确地更新选区，将新加入的 Tab 字符也包含进来，这样才能保证下一次 Tab 操作的判断依然正确。

## 4. 总结

通过这次功能实现，我们不仅完成了了一个鲁棒的、行为正确的文本缩进功能，也对 Jetpack Compose 的事件处理机制和状态管理有了更深刻的理解。关键的经验是：

-   **事件处理的顺序至关重要**：`onPreviewKeyEvent` 是拦截并优先处理事件的有力工具。
-   **状态更新的原子性**：Compose 中的状态应该是不可变的。每次操作都应该基于当前状态生成一个全新的状态（`TextFieldValue`），而不是试图在原地修改，这能有效避免许多难以追踪的 bug。
-   **清晰的逻辑分支**：面对复杂的交互，将逻辑清晰地划分为不同情况（如无选区、单行、多行、整行等）是保证代码正确性和可维护性的基础。

