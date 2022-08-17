package io.schiar.slowpoke.view

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.fragment.app.Fragment
import io.schiar.slowpoke.view.bluetooth.BluetoothService
import io.schiar.slowpoke.view.viewdata.DeviceViewData

fun Bundle?.println() {
    val extras = StringBuilder()
    if (this != null) {
        val keys = this.keySet()
        val it: Iterator<String> = keys.iterator()
        while (it.hasNext()) {
            val key = it.next()
            extras.append("$key = ${this[key]}\n")
        }
    }
    println(extras.toString())
}

fun Float.dp(displayMetrics: DisplayMetrics): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        displayMetrics
    )
}

fun Fragment.sendBluetoothServiceAction(action: Action, payload: Bundle? = null) {
    val serviceIntent = Intent(requireActivity(), BluetoothService::class.java)
    serviceIntent.action = action.name
    if (payload != null) {
        serviceIntent.putExtras(payload)
    }
    requireActivity().startService(serviceIntent)
}

fun Activity.sendBluetoothServiceAction(action: Action, payload: Bundle? = null) {
    val serviceIntent = Intent(this, BluetoothService::class.java)
    serviceIntent.action = action.name
    if (payload != null) {
        serviceIntent.putExtras(payload)
    }
    startService(serviceIntent)
}

@SuppressLint("MissingPermission")
fun BluetoothDevice.toDeviceViewData(bond: Boolean = false): DeviceViewData {
    return DeviceViewData(name, address, uuids?.map { it.toString() }?.toList() ?: listOf(), bond)
}
