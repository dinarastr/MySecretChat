package ru.yandexpraktikum.blechat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.yandexpraktikum.blechat.domain.model.Message

interface ChatRepository {
    fun getAllMessages(): Flow<List<Message>>
    suspend fun saveMessage(message: Message)
    fun getMessagesByDevice(deviceAddress: String): Flow<List<Message>>
}