package com.ririv.quickoutline.view.controls

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import java.util.UUID

class MessageContainerState {
    val messages = mutableStateListOf<MessageData>()

    fun showMessage(text: String, type: MessageType) {
        messages.add(MessageData(text, type))
    }
}

data class MessageData(
    val text: String,
    val type: MessageType,
    val id: UUID = UUID.randomUUID() // Add a unique ID
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageContainer(state: MessageContainerState, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) { // Use LazyColumn
        items(
            items = state.messages,
            key = { it.id } // Use the unique ID as the key
        ) { messageData ->
            Message(
                text = messageData.text,
                type = messageData.type,
                modifier = Modifier.animateItem()
            ) {
                state.messages.remove(messageData)
            }
        }
    }
}