package ru.yandexpraktikum.blechat.presentation.notifications

interface NotificationsHelper {

    fun createChannel(
        title: String,
        channelId: String,
    )

    fun notifyOnMessageReceived(
        title: String,
        message: String,
    )
}