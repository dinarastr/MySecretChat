package ru.yandexpraktikum.blechat.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

fun Context.checkForConnectPermission(action: () -> Unit) {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED -> action.invoke()
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> action.invoke()
    }
}