package ru.yandexpraktikum.blechat.presentation.notifications

interface NotificationsHelper {

    fun notifyOnMessageReceived(
        title: String,
        message: String,
    )
}