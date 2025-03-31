package ru.yandexpraktikum.blechat.presentation.notifications

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.yandexpraktikum.blechat.R
import javax.inject.Inject

private const val CHANNEL_ID = "channel_id"
private const val NOTIFICATION_ID = 1234

class NotificationsHelperImpl
@Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat,
): NotificationsHelper {

    override fun createChannel(
        title: String,
        channelId: String,
    ) {
        val channel = NotificationChannelCompat.Builder(/*id=*/channelId,
            NotificationManager.IMPORTANCE_DEFAULT
        )
            .setName(title)
            .build()
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    override fun notifyOnMessageReceived(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.bluetooth_ic)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}