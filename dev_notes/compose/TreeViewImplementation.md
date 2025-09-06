# Compose Desktop 树状视图实现文档

日期: 2025-09-05

本文档概述了在 Compose for Desktop 中一个可复用且带动画效果的树状视图组件的实现方法。

## 核心概念

该树状视图基于以下几个核心概念构建：

1.  **扁平化列表 (Flattened List):** 我们没有为每个节点使用递归的可组合函数，而是将整个树形结构“拍平”成一个列表 (`List`)。这使我们能够利用 `LazyColumn` 来实现高效的滚动和虚拟化，这对于大型树结构至关重要。

2.  **带状态的展开 (Stateful Expansion):** 每个节点的展开状态（无论是展开还是折叠）都在一个 `mutableStateMapOf` 中进行外部管理。这个映射表为每个 `Bookmark` 对象存储一个布尔值，以表示其展开状态。

3.  **动态扁平化 (Dynamic Flattening):** 我们使用一个 `flattenBookmarks` 函数来创建扁平化列表。该函数递归地遍历树，并且只将被状态映射表标记为“已展开”的节点的子项包含进来。每当展开状态发生变化时，此函数都会被重新调用，为 `LazyColumn` 生成一个新的扁平化列表。

4.  **动画 (Animations):** 我们在树的每一行上使用 `animateContentSize` 修饰符，以提供平滑的展开/折叠动画。当一个节点被展开或折叠时，其内容的大小会发生变化，该修饰符会自动为这个尺寸变化添加动画。

## 实现细节

### 1. 数据结构

树是根据 `Bookmark` 对象列表构建的，其中每个 `Bookmark` 都可以包含一个子 `Bookmark` 列表。

```kotlin
data class Bookmark(
    var title: String,
    var offsetPageNum: Optional<Int>,
    val level: Int,
    val children: ObservableList<Bookmark> = FXCollections.observableArrayList(),
    var parent: Bookmark? = null
)
```

### 2. 状态管理

我们使用 `mutableStateMapOf<Bookmark, Boolean>` 来存储每个节点的展开状态。这个映射表在 `BookmarkTree` 可组合函数中被创建并记忆。

```kotlin
val expansionState = remember { mutableStateMapOf<Bookmark, Boolean>() }
```

### 3. 扁平化树结构

`flattenBookmarks` 函数接收根书签列表和 `expansionState` 映射表作为输入，并返回一个用于显示的扁平化书签列表。

```kotlin
fun flattenBookmarks(
    bookmarks: List<Bookmark>,
    expansionState: Map<Bookmark, Boolean>
): List<Bookmark> {
    val flattenedList = mutableListOf<Bookmark>()
    for (bookmark in bookmarks) {
        flattenedList.add(bookmark)
        if (expansionState[bookmark] == true && bookmark.children.isNotEmpty()) {
            flattenedList.addAll(flattenBookmarks(bookmark.children, expansionState))
        }
    }
    return flattenedList
}
```

### 4. 显示树

我们使用 `LazyColumn` 来显示扁平化的书签列表。`LazyColumn` 中的每一行代表一个书签。

```kotlin
LazyColumn {
    items(flattenedBookmarks) { bookmark ->
        // ... 行内容
    }
}
```

### 5. 展开/折叠功能

我们使用一个 `Icon` 来表示展开/折叠的句柄。`Icon` 上的 `clickable` 修饰符用于在 `expansionState` 映射表中切换书签的展开状态。

```kotlin
if (bookmark.children.isNotEmpty()) {
    Icon(
        imageVector = if (expansionState[bookmark] == true) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
        contentDescription = "Expand/Collapse",
        modifier = Modifier.clickable { expansionState[bookmark] = !(expansionState[bookmark] ?: false) }
    )
}
```

### 6. “全部展开” / “全部折叠”

我们在标题行中提供了一个“全部展开”/“全部折叠”图标按钮。这个按钮通过切换 `expandAll` 状态来触发对 `expansionState` 映射表的更新，从而展开或折叠所有节点。

```kotlin
var expandAll by remember { mutableStateOf(true) }
// ...
val interactionSource = remember { MutableInteractionSource() }
val isHovered by interactionSource.collectIsHoveredAsState()
Icon(
    imageVector = if (expandAll) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
    tint = if (isHovered) Color.Black else Color.Gray,
    contentDescription = if (expandAll) "Collapse All" else "Expand All",
    modifier = Modifier.size(24.dp).clickable(
        interactionSource = interactionSource,
        indication = null, // No ripple effect
        onClick = {
            expandAll = !expandAll
            if (expandAll) {
                allBookmarks.forEach { expansionState[it] = true }
            } else {
                expansionState.clear()
            }
        }
    )
)
```

一个辅助函数 `getAllBookmarks` 用于获取树中所有书签的扁平列表，该列表用于初始化 `expansionState` 映射表并实现“全部展开”功能。

```kotlin
private fun getAllBookmarks(bookmarks: List<Bookmark>): List<Bookmark> {
    val allBookmarks = mutableListOf<Bookmark>()
    for (bookmark in bookmarks) {
        allBookmarks.add(bookmark)
        if (bookmark.children.isNotEmpty()) {
            allBookmarks.addAll(getAllBookmarks(bookmark.children))
        }
    }
    return allBookmarks
}
```

### 7. 处理新文件加载

为了确保每次加载新文件时树都默认完全展开，我们使用 `LaunchedEffect`。这个 Effect 会在 `bookmarks` 列表（即我们的输入数据）发生变化时执行。

```kotlin
LaunchedEffect(bookmarks) {
    expansionState.clear()
    allBookmarks.forEach { expansionState[it] = true }
    expandAll = true
}
```

这段代码做了三件事：
1.  `expansionState.clear()`: 清除旧的展开状态。
2.  `allBookmarks.forEach { expansionState[it] = true }`: 将所有新书签的状态设置成“展开”。
3.  `expandAll = true`: 重置“全部展开/折叠”按钮的状态，确保其与树的实际状态保持一致。

通过这种方式，我们保证了用户在打开新文件时总能看到一个完全展开的树视图，提供了统一且可预测的用户体验。