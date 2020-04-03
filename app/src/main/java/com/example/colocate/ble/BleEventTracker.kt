package com.example.colocate.ble

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BleEventTracker : BleEvents {

    private val eventsList = mutableListOf<ConnectedDevice>()

    private val connectionEvents = MutableLiveData<List<ConnectedDevice>>()

    override fun observeConnectionEvents(): LiveData<List<ConnectedDevice>> =
        connectionEvents

    override fun connectedDeviceEvent(id: String, rssiValues: List<Int>) {
        eventsList.add(
            ConnectedDevice(
                id = id,
                timestamp = getCurrentTimeStamp(),
                rssiValues = rssiValues
            )
        )
        connectionEvents.postValue(eventsList)
    }

    override fun disconnectDeviceEvent(id: String?) {
        eventsList.add(ConnectedDevice(isConnectionError = true, disconnectedDevice = id))
        connectionEvents.postValue(eventsList)
    }

    override fun scanFailureEvent() {
        eventsList.add(ConnectedDevice(isReadFailure = true))
        connectionEvents.postValue(eventsList)
    }

    override fun clear() {
        eventsList.clear()
        connectionEvents.postValue(eventsList)
    }
}

interface BleEvents {

    fun observeConnectionEvents(): LiveData<List<ConnectedDevice>>

    fun connectedDeviceEvent(id: String, rssiValues: List<Int>)

    fun disconnectDeviceEvent(id: String? = null)

    fun scanFailureEvent()

    fun clear()
}

fun getCurrentTimeStamp() = Date().toTimestamp()

fun Date.toTimestamp(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK).run {
        timeZone = TimeZone.getTimeZone("UTC")
        format(this@toTimestamp)
    }

data class ConnectedDevice(
    val id: String? = null,
    val timestamp: String = "",
    val rssiValues: List<Int> = emptyList(),
    val isConnectionError: Boolean = false,
    val isReadFailure: Boolean = false,
    val disconnectedDevice: String? = null
)
