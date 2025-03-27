package ru.yandexpraktikum.blechat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.yandexpraktikum.blechat.presentation.chats.incomingchat.IncomingChatScreen
import ru.yandexpraktikum.blechat.presentation.chats.outcomingchat.OutcomingChatScreen
import ru.yandexpraktikum.blechat.presentation.scanner.ScannedDevicesListScreen

/**
 * TODO("Add documentation")
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.ScannedDeviceList.route
    ) {
        composable(Screen.ScannedDeviceList.route) {
            ScannedDevicesListScreen(
                onDeviceClick = { deviceAddress, isIncomingChat ->
                    if (isIncomingChat) {
                       navController.navigate(Screen.IncomingChat.createRoute(deviceAddress))
                    } else {
                        navController.navigate(Screen.Chat.createRoute(deviceAddress))
                    }
                },
                onNavigateUp = {
                    navController.navigateUp()
                }
            )
        }

        composable(
            route = Screen.IncomingChat.route
        ) { backStackEntry ->
            val deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
            requireNotNull(deviceAddress) { "Device address cannot be null" }

            IncomingChatScreen(
                deviceAddress = deviceAddress,
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.Chat.route
        ) { backStackEntry ->
            val deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
            requireNotNull(deviceAddress) { "Device address cannot be null" }

            OutcomingChatScreen(
                deviceAddress = deviceAddress,
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}