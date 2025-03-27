package ru.yandexpraktikum.blechat.presentation.navigation

sealed class Screen(val route: String) {
    data object ScannedDeviceList : Screen("scanned_device_list")
    data object Chat : Screen("chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }
    data object IncomingChat : Screen("incoming_chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "incoming_chat/$deviceAddress"
    }
}