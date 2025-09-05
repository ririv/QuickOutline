package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class PopupTriggerType {
    INSTANT_ON_HOVER,
    DELAYED_ON_HOVER,
    CTRL_ON_ENTER,
    CTRL_WHILE_HOVER
}

enum class PopupPosition {
    TOP_CENTER,
    RIGHT_OF
}

@OptIn(ExperimentalComposeUiApi::class)
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
        hideJob?.cancel()
        hideJob = null
        if (!showPopup) {
            showJob = coroutineScope.launch {
                if (!instant) {
                    delay(showDelay)
                }
                showPopup = true
            }
        }
    }

    fun hide() {
        showJob?.cancel()
        showJob = null
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
            if (newBounds != ownerBounds) {
                ownerBounds = newBounds
            }
        }
        .onPointerEvent(PointerEventType.Enter) {
            val isCtrlPressed = it.keyboardModifiers.isCtrlPressed
            if (triggers.contains(PopupTriggerType.CTRL_ON_ENTER) && isCtrlPressed) {
                show(instant = true)
            } else if (triggers.contains(PopupTriggerType.INSTANT_ON_HOVER)) {
                show(instant = true)
            } else if (triggers.contains(PopupTriggerType.DELAYED_ON_HOVER)) {
                show()
            }
        }
        .onPointerEvent(PointerEventType.Exit) {
            hide()
        }
        .onPointerEvent(PointerEventType.Press) { // For CTRL_WHILE_HOVER
            val isCtrlPressed = it.keyboardModifiers.isCtrlPressed
            if (triggers.contains(PopupTriggerType.CTRL_WHILE_HOVER) && isCtrlPressed) {
                show(instant = true)
            }
        }

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
                        PopupPosition.RIGHT_OF -> ownerBounds.right + 5
                    }
                    var y = when (position) {
                        PopupPosition.TOP_CENTER -> ownerBounds.top - popupContentSize.height - 5
                        PopupPosition.RIGHT_OF -> ownerBounds.top
                    }

                    // Window boundary check
                    if (x + popupContentSize.width > windowSize.width) {
                        x = windowSize.width - popupContentSize.width
                    }
                    if (y + popupContentSize.height > windowSize.height) {
                        y = windowSize.height - popupContentSize.height
                    }
                    if (x < 0) {
                        x = 0
                    }
                    if (y < 0) {
                        y = 0
                    }

                    return IntOffset(x, y)
                }
            }
        }

        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = { showPopup = false }
        ) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.White.copy(alpha = 0.92f)
                )
            ) {
                Box(modifier = Modifier.onPointerEvent(PointerEventType.Enter) {
                    hideJob?.cancel()
                }.onPointerEvent(PointerEventType.Exit) {
                    hide()
                }) {
                    popupContent()
                }
            }
        }
    }
}