package ru.yandexpraktikum.blechat.di

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.yandexpraktikum.blechat.presentation.notifications.NotificationsHelper
import ru.yandexpraktikum.blechat.presentation.notifications.NotificationsHelperImpl
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
interface NotificationModule {

    @Binds
    @Singleton
    fun bindNotificationsHelper(impl: NotificationsHelperImpl): NotificationsHelper

    companion object {
        @Provides
        @Singleton
        fun provideNotificationManager(
            @ApplicationContext context: Context
        ): NotificationManagerCompat {
            return NotificationManagerCompat.from(context)
        }
    }
}