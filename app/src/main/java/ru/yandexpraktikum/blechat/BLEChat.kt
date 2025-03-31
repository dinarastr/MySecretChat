package ru.yandexpraktikum.blechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

private const val CHANNEL_ID = "channel_id"

@HiltAndroidApp
class BLEChat: Application() {
    override fun onCreate() {
        super.onCreate()
        setUpNotificationsChannel()
    }

    private fun setUpNotificationsChannel() {
        TODO()
    }
}