package ru.yandexpraktikum.blechat.domain.model

data class ScannedBluetoothDevice(
    val name: String?,
    val address: String,
    val isConnected: Boolean = false,
    val messages: List<Message> = emptyList()
) {
    fun addLocalMessage(text: String, localAddress: String?): ScannedBluetoothDevice {
        return this.copy(
            messages = this.messages + Message(
                text = text,
                senderAddress = localAddress.orEmpty(),
                isFromLocalUser = true
            )
        )
    }

    fun addRemoteMessage(text: String, senderAddress: String?): ScannedBluetoothDevice {
        return this.copy(
            messages = this.messages + Message(
                text = text,
                senderAddress = senderAddress.orEmpty(),
                isFromLocalUser = false
            )
        )
    }
}