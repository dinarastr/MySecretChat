package ru.yandexpraktikum.blechat.presentation.navigation

sealed class Screen(val route: String) {
    data object PairedDeviceList : Screen("paired_device_list")
    data object ScannedDeviceList : Screen("scanned_device_list")
    data object Chat : Screen("chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }
}