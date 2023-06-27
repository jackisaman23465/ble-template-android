package com.jagertech.ble_template

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

enum class PermissionStatus {
    DENIED,
    EXPLAINED,
}

inline fun AppCompatActivity.requestMultiplePermissions(
    permissions: Array<String>,
    crossinline allGranted: () -> Unit = {},
    crossinline denied: (List<String>) -> Unit = {},
    crossinline explained: (List<String>) -> Unit = {}
) {
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result: Map<String, Boolean> ->
        val deniedList = result.filter { !it.value }.map { it.key }
        when {
            deniedList.isNotEmpty() -> {
                val map = deniedList.groupBy { permission ->
                    if (shouldShowRequestPermissionRationale(permission)) PermissionStatus.DENIED else PermissionStatus.EXPLAINED
                }

                map[PermissionStatus.DENIED]?.let { denied.invoke(it) }

                map[PermissionStatus.EXPLAINED]?.let { explained.invoke(it) }
            }
            else -> allGranted.invoke()
        }
    }.launch(permissions)
}