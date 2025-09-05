# PopupCard 组件的定位问题、限制与方案取舍

## 1. 问题背景

在将应用从 JavaFX 迁移到 Jetpack Compose 的过程中，我们需要实现一个可复用的 `PopupCard` 组件。该组件需要能附加到任何其他组件上，并在其上方或侧方显示一个弹窗。然而，在实现过程中，我们遇到了两个严重的问题：

1.  **定位偏移**：当弹窗设置为在目标组件上方居中（`TOP_CENTER`）时，其实际位置总是会发生偏移，无法精确对齐。
2.  **组件变形**：在某些布局下（特别是当目标组件在 `Row` 中并使用了 `weight` 修饰符时），鼠标悬浮在目标组件上时，会导致其自身发生非预期的形变。

## 2. 失败的尝试与探索

为了解决这些问题，我们进行了多次尝试，但都未能完美解决：

### 尝试 1: `attachTo` Modifier 传递模式

*   **思路**：`PopupCard` 提供一个 `modifier`，由调用者手动应用到目标组件上。
*   **问题**：这种方式虽然灵活，但 `modifier` 上附带的 `onGloballyPositioned` 布局测量事件，在与 `weight` 等动态布局结合时，会引发不可预知的副作用，导致了按钮变形问题。

### 尝试 2: `Box` 包裹模式

*   **思路**：`PopupCard` 作为一个 `Box` 容器，将目标组件作为 `content` 包裹起来。
*   **问题**：这种方式下，我们测量到的是外层 `Box` 的边界，而不是内部目标组件的真实边界。当 `Box` 因为 `weight` 被拉伸时，我们基于 `Box` 的边界计算出的居中位置是错误的，导致了定位偏移。

### 尝试 3: 在使用处内联逻辑

*   **思路**：放弃 `PopupCard` 的封装，直接在 `BookmarkBottomPane` 中为每个按钮手动实现弹窗逻辑。
*   **问题**：这同样遇到了与尝试2一样的问题，即无法在 `weight` 布局中精确测量按钮的真实边界，偏移问题依旧存在。

## 3. 根本原因分析

所有失败尝试都指向了同一个根源：**我们无法在一个动态的、由父级决定的布局（如 `weight`）中，预先知道一个子组件最终会被渲染在哪个位置、拥有多大的尺寸。**

所有基于 `onGloballyPositioned` 的测量，要么因为测量了错误的容器而导致定位不准，要么因为对目标组件施加了 `modifier` 而导致其布局异常。

## 4. 为何放弃 `Layout` 方案

我们曾尝试用 `Layout`/`SubcomposeLayout` 解决定位精度问题，但在实际交互里暴露出关键缺陷：

- `Layout` 会把按钮（mainContent）和弹窗（popupContent）绘制在同一个图形层上，二者共享同一事件空间。
- 这使得我们无法为弹窗内容单独附加鼠标事件监听器（例如在弹窗上“悬停保持显示”）。
- 结果是：当鼠标从按钮移入弹窗区域时，按钮收不到 Hover，触发了“隐藏弹窗”，导致弹窗刚出现就消失，无法交互。

鉴于该问题影响核心交互，我们最终放弃了 `Layout` 方案。

## 5. 当前实现：基于 `Popup` 的定位与事件封装

我们回退到 Compose 的 `Popup` 组件：

核心要点如下：

- 锚点测量：在主内容（按钮/缩略图）的修饰符上使用 `onGloballyPositioned` 获取组件在“窗口坐标系”中的位置与尺寸；修复了 `IntRect` 的构造错误（`right = left + width`, `bottom = top + height`），避免定位偏移。
- 触发管理：支持 `INSTANT_ON_HOVER`、`DELAYED_ON_HOVER`、`CTRL_ON_ENTER`、`CTRL_WHILE_HOVER`，通过协程与 `showDelay`/`hideDelay` 实现延时显示与延时隐藏，并在弹窗区域 `Enter` 时取消隐藏任务。
- 位置计算：使用自定义 `PopupPositionProvider`，根据 `PopupPosition.TOP_CENTER` / `RIGHT_OF` 计算 `x/y`，并在窗口内进行边界夹紧（避免越界）。
- 交互稳定性：弹窗内容区域监听 `Enter/Exit`，进入时取消隐藏，离开时触发延时隐藏，避免轻微抖动时闪烁。

这套实现能够在“窗口内部”稳定工作，但存在一个框架级限制（见下一节）。

### 核心代码摘录（与当前实现一致）

```kotlin
@Composable
fun PopupCard(
	popupContent: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	triggers: Set<PopupTriggerType> = setOf(PopupTriggerType.DELAYED_ON_HOVER),
	position: PopupPosition = PopupPosition.RIGHT_OF,
	showDelay: Long = 1000,
	hideDelay: Long = 200,
	mainContent: @Composable (modifier: Modifier) -> Unit,
) {
	var showPopup by remember { mutableStateOf(false) }
	val coroutineScope = rememberCoroutineScope()
	var showJob by remember { mutableStateOf<Job?>(null) }
	var hideJob by remember { mutableStateOf<Job?>(null) }
	var ownerBounds by remember { mutableStateOf(IntRect.Zero) }

	fun show(instant: Boolean = false) {
		hideJob?.cancel(); hideJob = null
		if (!showPopup) {
			showJob = coroutineScope.launch {
				if (!instant) delay(showDelay)
				showPopup = true
			}
		}
	}
	fun hide() {
		showJob?.cancel(); showJob = null
		if (showPopup) {
			hideJob = coroutineScope.launch {
				delay(hideDelay)
				showPopup = false
			}
		}
	}

	val pointerModifier = modifier
		.onGloballyPositioned {
			val left = it.positionInWindow().x.roundToInt()
			val top = it.positionInWindow().y.roundToInt()
			val right = left + it.size.width
			val bottom = top + it.size.height
			val newBounds = IntRect(left, top, right, bottom)
			if (newBounds != ownerBounds) ownerBounds = newBounds
		}
		.onPointerEvent(PointerEventType.Enter) {
			val ctrl = it.keyboardModifiers.isCtrlPressed
			when {
				PopupTriggerType.CTRL_ON_ENTER in triggers && ctrl -> show(instant = true)
				PopupTriggerType.INSTANT_ON_HOVER in triggers -> show(instant = true)
				PopupTriggerType.DELAYED_ON_HOVER in triggers -> show()
			}
		}
		.onPointerEvent(PointerEventType.Exit) { hide() }
		.onPointerEvent(PointerEventType.Press) {
			val ctrl = it.keyboardModifiers.isCtrlPressed
			if (PopupTriggerType.CTRL_WHILE_HOVER in triggers && ctrl) show(instant = true)
		}

	// 渲染主内容，向外暴露 modifier 以便调用侧可叠加尺寸/对齐等修饰符
	mainContent(pointerModifier)

	if (showPopup) {
		val positionProvider = remember(position, ownerBounds) {
			object : PopupPositionProvider {
				override fun calculatePosition(
					anchorBounds: IntRect,
					windowSize: IntSize,
					layoutDirection: LayoutDirection,
					popupContentSize: IntSize
				): IntOffset {
					var x = when (position) {
						PopupPosition.TOP_CENTER -> ownerBounds.left + (ownerBounds.width - popupContentSize.width) / 2
						PopupPosition.RIGHT_OF   -> ownerBounds.right + 5
					}
					var y = when (position) {
						PopupPosition.TOP_CENTER -> ownerBounds.top - popupContentSize.height - 5
						PopupPosition.RIGHT_OF   -> ownerBounds.top
					}

					// Window boundary check (inside the window only)
					if (x + popupContentSize.width > windowSize.width) x = windowSize.width - popupContentSize.width
					if (y + popupContentSize.height > windowSize.height) y = windowSize.height - popupContentSize.height
					if (x < 0) x = 0
					if (y < 0) y = 0
					return IntOffset(x, y)
				}
			}
		}

		Popup(popupPositionProvider = positionProvider, onDismissRequest = { showPopup = false }) {
			ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.92f))) {
				Box(
					modifier = Modifier
						.onPointerEvent(PointerEventType.Enter) { hideJob?.cancel() }
						.onPointerEvent(PointerEventType.Exit) { hide() }
				) {
					popupContent()
				}
			}
		}
	}
}
```

参数说明：

- `position`: `TOP_CENTER` | `RIGHT_OF`；
- `triggers`: 触发集合；
- `showDelay`/`hideDelay`: 显隐延迟，毫秒。

## 6. 已知限制：Compose Desktop 的 `Popup` 无法越出所属窗口

在 Desktop（macOS/Windows/Linux）上，`Popup` 的绘制被限制在所属窗口的图形层内，无法真正“溢出”到主窗口之外显示。即使关闭我们自定义的边界夹紧（`clampToWindowBounds=false`），也不会突破窗口边界。

这正是“缩略图预览希望在主窗口右侧（窗口外）显示，却被挤回窗口内”的根因，属于框架行为而非业务逻辑问题。

## 7. 解决方案（暂未实现）

若必须在窗口外显示较大的预览/浮层，推荐方案是：

1) 独立无边框顶层窗口（推荐）
- 在悬停触发时，创建一个无边框、非模态的小窗，置顶显示，位置锚定到目标组件的屏幕绝对坐标（目标在窗口内的位置 + 主窗口的 `locationOnScreen`）。
- 需处理多显示器与 HiDPI 缩放、焦点与鼠标事件（进入窗口保持显示、离开后延时关闭）。
- 该方案不受主窗口边界限制，可实现“窗口外”显示。
- 状态：暂未实现。

2) 窗口内的回退策略（权衡）
- 智能翻转（靠右放不下就放左，靠上放不下就放下）、自适应限宽/缩放，使内容在窗口内尽量不遮挡关键区域。
- 该策略无法突破窗口边界，仅改善窗口内体验。
- 状态：视需求选择性实施。

## 8. 结论
- 如需“窗口外”显示，需改造为独立无边框顶层窗口（暂未实现）。

## 9. 用法示例（当前实现）

### 9.1 顶部居中弹出（按钮）：`BookmarkBottomPane.kt`

```kotlin
Box(modifier = Modifier.weight(1f)) {
	PopupCard(
		popupContent = { GetContentsPopupContent { /* 更新选择 */ } },
		triggers = setOf(PopupTriggerType.INSTANT_ON_HOVER),
		position = PopupPosition.TOP_CENTER,
		modifier = Modifier.align(Alignment.Center)
	) { modifier ->
		StyledButton(
			onClick = { /* 执行动作 */ },
			text = stringResource("bookmarkTab.getContentsBtn.text"),
			type = ButtonType.PLAIN_PRIMARY,
			modifier = modifier.width(120.dp)
		)
	}
}
```

说明：
- `PopupCard` 的 `modifier` 用于在父容器中对齐；
- 主内容通过回调参数 `modifier` 承接事件监听与布局测量，不要丢失；
- 悬停立即显示，弹窗在按钮上方居中。

### 9.2 右侧预览弹出（缩略图）：`ThumbnailPane.kt`

```kotlin
PopupCard(
	popupContent = {
		LaunchedEffect(Unit) { viewModel.getPreviewImage(index) { previewImage = it } }
		if (previewImage != null) {
			Image(bitmap = previewImage!!, contentDescription = null,
				  modifier = Modifier.width(600.dp).clip(RoundedCornerShape(4.dp)))
		} else {
			Box(Modifier.size(600.dp, 850.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
				Text("Loading...")
			}
		}
	},
	position = PopupPosition.RIGHT_OF,
	triggers = setOf(
		PopupTriggerType.DELAYED_ON_HOVER,
		PopupTriggerType.CTRL_ON_ENTER,
		PopupTriggerType.CTRL_WHILE_HOVER
	),
	hideDelay = 1L
) { modifier ->
	Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
		// 缩略图卡片 ...
	}
}
```

说明：
- 预览使用右侧弹出；Desktop 端依然无法越出窗口（框架限制，见第6节）。

- `Layout` 方案因事件层问题被放弃：弹窗和按钮在同一层，无法实现稳定的“移入弹窗保持显示”。
- 当前使用的 `Popup` 方案可稳定工作于窗口内，但存在桌面端的“不可越界”限制。
- 如需“窗口外”显示，需改造为独立无边框顶层窗口（暂未实现）。