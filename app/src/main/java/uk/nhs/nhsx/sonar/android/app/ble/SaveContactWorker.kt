/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEvent
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import javax.inject.Inject
import javax.inject.Named

class SaveContactWorker @Inject constructor(
    @Named(AppModule.DISPATCHER_IO) private val dispatcher: CoroutineDispatcher,
    private val contactEventDao: ContactEventDao
) {

    fun createOrUpdateContactEvent(
        scope: CoroutineScope,
        id: ByteArray,
        rssi: Int,
        timestamp: DateTime
    ) {
        scope.launch {
            withContext(dispatcher) {
                try {
                    Timber.e("saving ${id.size} rssi=$rssi timestamp=${timestamp.toUtcIsoFormat()}")
                    if (id.size != BluetoothIdentifier.SIZE) {
                        throw IllegalArgumentException("Identifier has wrong size, must be ${BluetoothIdentifier.SIZE}, was ${id.size}")
                    }
                    val bluetoothIdentifier = BluetoothIdentifier.fromBytes(id)

                    val contactEvent =
                        ContactEvent(
                            sonarId = bluetoothIdentifier.cryptogram.asBytes(),
                            rssiValues = listOf(rssi),
                            rssiTimestamps = listOf(timestamp.millis),
                            timestamp = timestamp.millis,
                            duration = 60,
                            txPowerInProtocol = bluetoothIdentifier.txPower,
                            txPowerAdvertised = 0,
                            transmissionTime = bluetoothIdentifier.transmissionTime,
                            countryCode = bluetoothIdentifier.countryCode,
                            hmacSignature = bluetoothIdentifier.hmacSignature
                        )
                    contactEventDao.createOrUpdate(contactEvent)
                } catch (e: Exception) {
                    Timber.e("$TAG Failed to save with exception $e")
                }
            }
        }
    }

    companion object {
        const val TAG = "SaveWorker"
    }
}
