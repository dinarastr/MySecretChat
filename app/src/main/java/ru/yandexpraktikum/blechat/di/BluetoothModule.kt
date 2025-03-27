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
import ru.yandexpraktikum.blechat.data.bluetooth.BLEClientControllerImpl
import ru.yandexpraktikum.blechat.data.bluetooth.BLEServerControllerImpl
import ru.yandexpraktikum.blechat.domain.bluetooth.BLEClientController
import ru.yandexpraktikum.blechat.domain.bluetooth.BLEServerController
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothModule {

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
    abstract fun provideServerController(
        serverControllerImpl: BLEServerControllerImpl
    ): BLEServerController

    @Binds
    @Singleton
    abstract fun provideClientController(
        clientControllerImpl: BLEClientControllerImpl
    ): BLEClientController
}