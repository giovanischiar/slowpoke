package io.schiar.slowpoke.view.listeners

import android.bluetooth.BluetoothDevice

interface OnDeviceFoundListener {
    fun onDeviceFoundListener(device: BluetoothDevice, bond: Boolean)
}
