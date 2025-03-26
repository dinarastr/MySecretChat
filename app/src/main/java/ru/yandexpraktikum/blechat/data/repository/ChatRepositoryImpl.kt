package ru.yandexpraktikum.blechat.data.repository

import kotlinx.coroutines.flow.Flow
import ru.yandexpraktikum.blechat.data.local.MessageDataSource
import ru.yandexpraktikum.blechat.domain.model.Message
import ru.yandexpraktikum.blechat.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageDataSource: MessageDataSource
) : ChatRepository {
    
    override fun getAllMessages(): Flow<List<Message>> {
        return messageDataSource.getAllMessages()
    }

    override suspend fun saveMessage(message: Message) {
        messageDataSource.addMessage(message)
    }

    override fun getMessagesByDevice(deviceAddress: String): Flow<List<Message>> {
        return messageDataSource.getMessagesByDevice(deviceAddress)
    }
}