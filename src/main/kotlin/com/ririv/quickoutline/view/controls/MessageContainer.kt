package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

class MessageContainerState {
    val messages = mutableStateListOf<MessageData>()

    fun showMessage(text: String, type: MessageType) {
        messages.add(MessageData(text, type))
    }
}

data class MessageData(val text: String, val type: MessageType)

@Composable
fun MessageContainer(state: MessageContainerState) {
    Column {
        state.messages.forEach { messageData ->
            Message(text = messageData.text, type = messageData.type) {
                state.messages.remove(messageData)
            }
        }
    }
}

@Composable
fun rememberMessageContainerState() = remember { MessageContainerState() }