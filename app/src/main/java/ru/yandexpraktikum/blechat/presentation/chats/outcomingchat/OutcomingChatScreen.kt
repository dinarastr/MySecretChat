package ru.yandexpraktikum.blechat.presentation.chats.outcomingchat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.yandexpraktikum.blechat.domain.model.Message
import ru.yandexpraktikum.blechat.presentation.chats.ChatEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutcomingChatScreen(
    deviceAddress: String,
    onNavigateUp: () -> Unit,
    viewModel: OutcomingChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(deviceAddress) {
        // Initialize chat with the connected device
        viewModel.onEvent(ChatEvent.LoadMessages(deviceAddress))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.connectedDevice?.name ?: "Chat") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Navigate back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true
                ) {
                    items(state.messages.reversed()) { message ->
                        MessageItem(
                            message = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message") }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            viewModel.onEvent(
                                ChatEvent.SendMessage(
                                    messageText,
                                    address = deviceAddress
                                )
                            )
                            messageText = ""
                        },
                        enabled = messageText.isNotBlank() && state.connectedDevice != null
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (message.isFromLocalUser) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = if (message.isFromLocalUser)
                Alignment.End
            else
                Alignment.Start
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}