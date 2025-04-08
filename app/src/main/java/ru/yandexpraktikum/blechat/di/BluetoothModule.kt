package ru.yandexpraktikum.blechat.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.yandexpraktikum.blechat.data.bluetooth.BleClientControllerImpl
import ru.yandexpraktikum.blechat.data.bluetooth.BleServerControllerImpl
import ru.yandexpraktikum.blechat.domain.bluetooth.BleClientController
import ru.yandexpraktikum.blechat.domain.bluetooth.BleServerController
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface BluetoothModule {

    companion object {

        @Provides
        @Singleton
        fun provideBluetoothManager(
            @ApplicationContext context: Context
        ): BluetoothManager? {
            return context.getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        }

        @Provides
        @Singleton
        fun provideBluetoothAdapter(
            bluetoothManager: BluetoothManager?
        ): BluetoothAdapter? {
            return bluetoothManager?.adapter
        }
    }

    @Binds
    @Singleton
    fun provideServerController(
        serverControllerImpl: BleServerControllerImpl
    ): BleServerController

    @Binds
    @Singleton
    fun provideClientController(
        clientControllerImpl: BleClientControllerImpl
    ): BleClientController
}