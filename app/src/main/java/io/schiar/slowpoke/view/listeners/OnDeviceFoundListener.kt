package io.schiar.slowpoke.view.listeners

import android.bluetooth.BluetoothDevice

interface OnDeviceFoundListener {
    fun onDeviceFind(device: BluetoothDevice, bond: Boolean)
}
