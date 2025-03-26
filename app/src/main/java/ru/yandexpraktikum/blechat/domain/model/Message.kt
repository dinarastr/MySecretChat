package ru.yandexpraktikum.blechat.domain.model

data class Message(
    val text: String,
    val senderAddress: String,
    val isFromLocalUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)