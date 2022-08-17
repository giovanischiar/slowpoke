package io.schiar.slowpoke.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.schiar.slowpoke.view.viewdata.DeviceViewData
import io.schiar.slowpoke.view.viewdata.MessageViewData

class MessagesViewModel : ViewModel() {
    private var messagesMap = mapOf<String, List<MessageViewData>>()
    private var devicesMap = mapOf<String, DeviceViewData>()

    val devices: MutableLiveData<Map<String, DeviceViewData>> by lazy {
        MutableLiveData(mutableMapOf())
    }
    var remoteDevice = MutableLiveData<DeviceViewData>()
    var clientDevice = MutableLiveData<DeviceViewData>()
    var messages = MutableLiveData<List<Pair<DeviceViewData, MessageViewData>>>()
    var currentConversation = MutableLiveData<List<MessageViewData>>()

    // MainActivity methods
    fun remoteDeviceWasConnected(name: String, macAddress: String, uuids: List<String>, bond: Boolean) {
        val deviceViewData = DeviceViewData(name, macAddress, uuids, bond)
        devicesMap = devicesMap + mapOf(macAddress to deviceViewData)
        devices.postValue(devicesMap)
        remoteDevice.postValue(deviceViewData)
    }

    private fun addMessagesFromRemoteDevice(address: String, msg: String, origin: Boolean) {
        val messagesFromRemoteDevice = messagesMap[address] ?: listOf()
        val messageHistory = messagesFromRemoteDevice + MessageViewData(origin, msg)
        messagesMap = messagesMap + mapOf(address to messageHistory)
        messages.postValue(messagesMap.map { (devicesMap[it.key] ?: return) to it.value.last() })
    }

    fun messageWasReceived(msg: String) {
        val remoteDevice = this.remoteDevice.value ?: return
        val address = remoteDevice.macAddress
        addMessagesFromRemoteDevice(address, msg, false)
        currentConversation.postValue(messagesMap[address])
    }

    // ConversationFragment method
    fun messageWasSent(msg: String) {
        val remoteDevice = this.remoteDevice.value ?: return
        val address = remoteDevice.macAddress
        addMessagesFromRemoteDevice(address, msg, true)
        currentConversation.postValue(messagesMap[address])
    }

    // MessagesFragment methods
    fun newLastMessagesWasAdded(messages: Map<DeviceViewData, List<MessageViewData>>) {
        messages.keys.forEach {
            devicesMap = devicesMap + mapOf(it.macAddress to it)
            messagesMap = messagesMap + mapOf(it.macAddress to (messages[it] ?: listOf()))
        }
        // this.devices.postValue(devicesMap)
        this.messages.postValue(messagesMap.map { (devicesMap[it.key] ?: return) to it.value.last() })
    }

    fun messageWasClicked(address: String) {
        val device = devicesMap[address] ?: return
        val messages = messagesMap[address] ?: listOf()
        remoteDevice.postValue(device)
        currentConversation.postValue(messages)
    }

    // DevicesFragment methods
    fun addNewDevice(name: String, macAddress: String, uuids: List<String>, bond: Boolean) {
        val newDeviceViewData = DeviceViewData(name, macAddress, uuids, bond)
        if (devicesMap[macAddress] != newDeviceViewData) {
            devicesMap = devicesMap + mapOf(macAddress to newDeviceViewData)
            devices.postValue(devicesMap)
        }
    }

    fun deviceWasClicked(address: String) {
        clientDevice.postValue((devices.value ?: mutableMapOf())[address])
    }
}