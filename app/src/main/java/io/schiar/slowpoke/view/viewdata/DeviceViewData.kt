package io.schiar.slowpoke.view.viewdata

data class DeviceViewData(
    val name: String,
    val macAddress: String,
    val uuids: List<String>,
    val bond: Boolean
)