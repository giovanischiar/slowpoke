package io.schiar.slowpoke.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.schiar.slowpoke.view.listeners.*
import io.schiar.slowpoke.view.viewdata.DeviceViewData
import io.schiar.slowpoke.view.viewdata.MessageViewData

class MessagesViewModel :
    ViewModel(),
    OnMessageReceivedListener,
    OnMessageSentListener,
    OnDeviceClickedListener
{
    private var messageHistory = listOf<MessageViewData>()
    var newMessageViewData = MutableLiveData(messageHistory)
    val devices: MutableLiveData<MutableMap<String, DeviceViewData>> by lazy {
        MutableLiveData(mutableMapOf())
    }
    var remoteDevice = MutableLiveData<DeviceViewData>()
    var clientDevice = MutableLiveData<DeviceViewData>()

    override fun onMessageReceive(msg: String) {
        messageHistory = messageHistory + MessageViewData(false, msg)
        newMessageViewData.postValue(messageHistory)
    }

    override fun onMessageSend(msg: String) {
        messageHistory = messageHistory + MessageViewData(true, msg)
        newMessageViewData.postValue(messageHistory)
    }

    fun addNewDevice(name: String, macAddress: String, uuids: List<String>, bond: Boolean) {
        val value = (devices.value ?: mutableMapOf())
        val newDeviceViewData = DeviceViewData(name, macAddress, uuids, bond)
        if (value[macAddress] != newDeviceViewData) {
            value[macAddress] = newDeviceViewData
            devices.postValue(value)
        }
    }

    override fun onDeviceClicked(address: String) {
        clientDevice.postValue((devices.value ?: mutableMapOf())[address])
    }

    fun onRemoteDeviceConnected(name: String, macAddress: String, uuids: List<String>, bond: Boolean) {
        remoteDevice.postValue(DeviceViewData(name, macAddress, uuids, bond))
    }

}