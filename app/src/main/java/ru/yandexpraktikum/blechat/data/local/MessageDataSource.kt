package ru.yandexpraktikum.blechat.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.yandexpraktikum.blechat.domain.model.Message
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageDataSource @Inject constructor() {
    private val messages = MutableStateFlow<List<Message>>(emptyList())

    fun getAllMessages(): Flow<List<Message>> = messages

    fun getMessagesByDevice(deviceAddress: String): Flow<List<Message>> {
        return messages.map { messageList ->
            messageList.filter { it.senderAddress == deviceAddress }
        }
    }

    suspend fun addMessage(message: Message) {
        messages.update { currentMessages ->
            currentMessages + message
        }
    }
}