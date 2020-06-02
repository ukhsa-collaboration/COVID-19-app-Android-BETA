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
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

interface BleEventEmitter {
    fun successfulContactEvent(
        id: ByteArray,
        rssiValues: List<Int>,
        txPowerAdvertised: Int
    )

    fun errorEvent(
        macAddress: String,
        error: Throwable
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

    private val contactEventsList = mutableListOf<SuccessfulContactEvent>()
    private var errorEvents = mutableListOf<BleError>()
    private val contactEvents = MutableLiveData<List<SuccessfulContactEvent>>()
    private val lock = Object()

    fun observeContactEvents(): LiveData<List<SuccessfulContactEvent>> =
        contactEvents

    fun getErrors(): List<BleError> = errorEvents

    fun clear() {
        safelyUpdateConnectedDeviceEventList {
            contactEventsList.clear()
        }

        synchronized(lock) {
            errorEvents.clear()
        }
    }

    override fun successfulContactEvent(
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

        safelyUpdateConnectedDeviceEventList {
            val lastEvent = contactEventsList.firstOrNull { it.cryptogram == idString }
            if (lastEvent != null) {
                contactEventsList.remove(lastEvent)
                contactEventsList.add(
                    lastEvent.copy(
                        rssiTimestamps = listOf(getCurrentTimeStamp()) + lastEvent.rssiTimestamps,
                        rssiValues = rssiValues + lastEvent.rssiValues
                    )
                )
            } else {
                contactEventsList.add(
                    SuccessfulContactEvent(
                        cryptogram = idString,
                        firstSeen = getCurrentTimeStamp(),
                        rssiTimestamps = listOf(getCurrentTimeStamp()),
                        rssiValues = rssiValues,
                        txPowerAdvertised = txPowerAdvertised,
                        txPowerProtocol = identifier.txPower.toInt()
                    )
                )
            }
            contactEventsList.sortByDescending { it.firstSeen }
        }
    }

    override fun errorEvent(macAddress: String, error: Throwable) {
        synchronized(lock) {
            errorEvents.plusAssign(BleError(getCurrentTimeStamp().toUtcIsoFormat(), macAddress, error))
        }
    }

    private fun safelyUpdateConnectedDeviceEventList(codeBlock: () -> Unit) {
        val eventListCopy = synchronized(lock) {
            codeBlock()
            mutableListOf<SuccessfulContactEvent>().apply { addAll(contactEventsList) }
        }
        contactEvents.postValue(eventListCopy)
    }
}

@Singleton
class NoOpBleEventEmitter @Inject constructor() : BleEventEmitter {
    override fun successfulContactEvent(
        id: ByteArray,
        rssiValues: List<Int>,
        txPowerAdvertised: Int
    ) {
    }

    override fun errorEvent(macAddress: String, error: Throwable) {
    }
}

private fun getCurrentTimeStamp() = DateTime.now(DateTimeZone.UTC)

data class SuccessfulContactEvent(
    val cryptogram: String? = null,
    val firstSeen: DateTime = DateTime.now(),
    val rssiTimestamps: List<DateTime> = listOf(DateTime.now()),
    val rssiValues: List<Int> = emptyList(),
    val txPowerProtocol: Int = 0,
    val txPowerAdvertised: Int = 0,
    var expanded: Boolean = false
)

data class BleError(
    val timestamp: String,
    val macAddress: String,
    val error: Throwable
)
