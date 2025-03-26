package ru.yandexpraktikum.blechat.domain.model

sealed class ConnectionState {
    data object Connected : ConnectionState()
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Disconnecting : ConnectionState()
    data object Advertising : ConnectionState()
}