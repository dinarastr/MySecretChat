package ru.yandexpraktikum.blechat.presentation.chats

sealed class ChatEvent {
    data class LoadMessages(
        val address: String
    ): ChatEvent()

    data class SendMessage(
        val message: String,
        val address: String
    ): ChatEvent()
}