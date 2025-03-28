package ru.yandexpraktikum.blechat.presentation.chats.incomingchat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import ru.yandexpraktikum.blechat.presentation.chats.ChatEvent
import ru.yandexpraktikum.blechat.presentation.components.ChatScreen

@Composable
fun IncomingChatScreen(
    deviceAddress: String,
    onNavigateUp: () -> Unit,
    viewModel: IncomingChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.messages) {
        viewModel.onEvent(ChatEvent.LoadMessages(deviceAddress))
    }


    state.apply {
        ChatScreen(
            deviceAddress = deviceAddress,
            connectedDevice = connectedDevice,
            chatsTitle = connectedDevice?.name ?: deviceAddress,
            onSendMessage = { message, address ->
                viewModel.onEvent(ChatEvent.SendMessage(message, address))
            },
            onNavigateUp = onNavigateUp,
            messages = messages.reversed()
        )
    }
}