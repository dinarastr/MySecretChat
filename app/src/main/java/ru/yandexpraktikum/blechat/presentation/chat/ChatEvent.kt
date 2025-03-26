package ru.yandexpraktikum.blechat.presentation.chat

sealed class ChatEvent {
    data class SendMessage(
        val message: String,
        val address: String
    ): ChatEvent()
}