/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleEvents @Inject constructor() {
    constructor(customEncoder: (ByteArray) -> String) : this() {
        base64Encoder = customEncoder
    }

    var base64Encoder: (ByteArray) -> String = {
        Base64.encodeToString(it, Base64.DEFAULT)
    }

    private val eventsList = mutableListOf<ConnectedDevice>()
    private val connectionEvents = MutableLiveData<List<ConnectedDevice>>()
    private val lock = Object()

    fun observeConnectionEvents(): LiveData<List<ConnectedDevice>> =
        connectionEvents

    fun connectedDeviceEvent(id: ByteArray, rssiValues: List<Int>) {
        val identifier = try {
            BluetoothIdentifier.fromBytes(id)
        } catch (e: Exception) {
            return
        }
        val idString = base64Encoder(identifier.cryptogram.asBytes())
        safelyUpdateEventList {
            val lastEvent = eventsList.firstOrNull { it.id == idString }
            if (lastEvent != null) {
                eventsList.remove(lastEvent)
                eventsList.add(
                    lastEvent.copy(
                        timestamp = getCurrentTimeStamp(),
                        lastTimestamp = lastEvent.timestamp,
                        rssiValues = rssiValues
                    )
                )
            } else {
                eventsList.add(
                    ConnectedDevice(
                        id = idString,
                        timestamp = getCurrentTimeStamp(),
                        lastTimestamp = getCurrentTimeStamp(),
                        rssiValues = rssiValues
                    )
                )
            }
        }
    }

    fun scanFailureEvent() {
        safelyUpdateEventList {
            eventsList.add(ConnectedDevice(isReadFailure = true))
        }
    }

    fun clear() {
        safelyUpdateEventList {
            eventsList.clear()
        }
    }

    private fun safelyUpdateEventList(codeBlock: () -> Unit) {
        val eventListCopy = synchronized(lock) {
            codeBlock()
            mutableListOf<ConnectedDevice>().apply { addAll(eventsList) }
        }
        connectionEvents.postValue(eventListCopy)
    }
}

private fun getCurrentTimeStamp() = DateTime.now(DateTimeZone.UTC)

data class ConnectedDevice(
    val id: String? = null,
    val timestamp: DateTime = DateTime.now(),
    val lastTimestamp: DateTime = DateTime.now(),
    val rssiValues: List<Int> = emptyList(),
    val isConnectionError: Boolean = false,
    val isReadFailure: Boolean = false
)
