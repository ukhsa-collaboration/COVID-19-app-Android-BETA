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

interface BleEventEmitter {
    fun connectedDeviceEvent(
        id: ByteArray,
        rssiValues: List<Int>,
        txPowerAdvertised: Int
    )
}

@Singleton
class DebugBleEventTracker @Inject constructor() : BleEventEmitter {
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

    fun clear() {
        safelyUpdateEventList {
            eventsList.clear()
        }
    }

    override fun connectedDeviceEvent(
        id: ByteArray,
        rssiValues: List<Int>,
        txPowerAdvertised: Int
    ) {
        val identifier = try {
            BluetoothIdentifier.fromBytes(id)
        } catch (e: Exception) {
            return
        }
        val idString = base64Encoder(identifier.cryptogram.asBytes())
            .replace("\n", "")

        safelyUpdateEventList {
            val lastEvent = eventsList.firstOrNull { it.cryptogram == idString }
            if (lastEvent != null) {
                eventsList.remove(lastEvent)
                eventsList.add(
                    lastEvent.copy(
                        rssiTimestamps = listOf(getCurrentTimeStamp()) + lastEvent.rssiTimestamps,
                        rssiValues = rssiValues + lastEvent.rssiValues
                    )
                )
            } else {
                eventsList.add(
                    ConnectedDevice(
                        cryptogram = idString,
                        firstSeen = getCurrentTimeStamp(),
                        rssiTimestamps = listOf(getCurrentTimeStamp()),
                        rssiValues = rssiValues,
                        txPowerAdvertised = txPowerAdvertised,
                        txPowerProtocol = identifier.txPower.toInt()
                    )
                )
            }
            eventsList.sortByDescending { it.firstSeen }
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

@Singleton
class NoOpBleEventEmitter @Inject constructor() : BleEventEmitter {
    override fun connectedDeviceEvent(
        id: ByteArray,
        rssiValues: List<Int>,
        txPowerAdvertised: Int
    ) {
    }
}

private fun getCurrentTimeStamp() = DateTime.now(DateTimeZone.UTC)

data class ConnectedDevice(
    val cryptogram: String? = null,
    val firstSeen: DateTime = DateTime.now(),
    val rssiTimestamps: List<DateTime> = listOf(DateTime.now()),
    val rssiValues: List<Int> = emptyList(),
    val txPowerProtocol: Int = 0,
    val txPowerAdvertised: Int = 0,
    var expanded: Boolean = false
)
