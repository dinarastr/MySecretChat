package ru.yandexpraktikum.blechat.presentation.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val CHANNEL_ID = "channel_id"
private const val NOTIFICATION_ID = 1234

class NotificationsHelperImpl
@Inject constructor(
    @ApplicationContext private val context: Context,

) : NotificationsHelper {

    override fun notifyOnMessageReceived(title: String, message: String) {
            TODO()
    }
}