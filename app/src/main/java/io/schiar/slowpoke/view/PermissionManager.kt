package io.schiar.slowpoke.view

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import io.schiar.slowpoke.Permission

class PermissionManager(private val activity: FragmentActivity) {
    private val permissionToActivityRegister = mutableMapOf<String, ActivityResultLauncher<String>>()
    private var systemPermissionToPermission = mapOf(
        Permission.BLUETOOTH to Manifest.permission.BLUETOOTH,
        Permission.BLUETOOTH_ADMIN to Manifest.permission.BLUETOOTH_ADMIN,
        Permission.ACCESS_COARSE_LOCATION to Manifest.permission.ACCESS_COARSE_LOCATION,
        Permission.ACCESS_FINE_LOCATION to Manifest.permission.ACCESS_FINE_LOCATION,
        Permission.ACTION_REQUEST_ENABLE to BluetoothAdapter.ACTION_REQUEST_ENABLE,
        Permission.ACTION_REQUEST_DISCOVERABLE to BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE,
    ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) mapOf(
        Permission.BLUETOOTH_ADVERTISE to Manifest.permission.BLUETOOTH_ADVERTISE,
        Permission.BLUETOOTH_SCAN to Manifest.permission.BLUETOOTH_SCAN,
        Permission.BLUETOOTH_CONNECT to Manifest.permission.BLUETOOTH_CONNECT,
    ) else mapOf()
    private var sdkSPermissions = setOf(
        Permission.BLUETOOTH_ADVERTISE,
        Permission.BLUETOOTH_SCAN,
        Permission.BLUETOOTH_CONNECT
    )

    fun setPermissionListener(permission: Permission, onPermissionGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && sdkSPermissions.contains(permission)) {
            onPermissionGranted()
            return
        }
        val systemPermission = systemPermissionToPermission[permission] ?: return
        permissionToActivityRegister[systemPermission] = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                onPermissionGranted()
                return@registerForActivityResult
            }
            println("requestAuthorization $permission")
            requestAuthorization(permission)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val alreadyHasPermission = activity.checkSelfPermission(systemPermission)
            if (alreadyHasPermission == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
                return
            }
        }
        requestAuthorization(permission)
    }

    private fun requestAuthorization(permission: Permission) {
        val systemPermission = systemPermissionToPermission[permission]
        permissionToActivityRegister[systemPermission]?.launch(systemPermission)
        ActivityCompat.requestPermissions(activity, arrayOf(systemPermission), 1)
    }
}