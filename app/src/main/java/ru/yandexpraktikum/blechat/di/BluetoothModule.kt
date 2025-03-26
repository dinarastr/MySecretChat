package ru.yandexpraktikum.blechat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.yandexpraktikum.blechat.data.bluetooth.BluetoothControllerImpl
import ru.yandexpraktikum.blechat.domain.bluetooth.BluetoothController
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothModule {

    @Binds
    @Singleton
    abstract fun provideBluetoothController(
        bluetoothControllerImpl: BluetoothControllerImpl
    ): BluetoothController
}