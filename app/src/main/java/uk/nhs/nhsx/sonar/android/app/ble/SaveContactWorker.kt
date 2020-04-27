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
import javax.inject.Named

interface SaveContactWorker {
    fun createOrUpdateContactEvent(
        scope: CoroutineScope,
        id: ByteArray,
        rssi: Int,
        timestamp: DateTime
    )
}

class DefaultSaveContactWorker(
    @Named(AppModule.DISPATCHER_IO) private val dispatcher: CoroutineDispatcher,
    private val contactEventDao: ContactEventDao
) : SaveContactWorker {

    override fun createOrUpdateContactEvent(
        scope: CoroutineScope,
        id: ByteArray,
        rssi: Int,
        timestamp: DateTime
    ) {
        scope.launch {
            withContext(dispatcher) {
                try {
                    // Attempt to create identifier from payload to ensure correct structure
                    val identifier = BluetoothIdentifier.fromBytes(id)
                    val contactEvent =
                        ContactEvent(
                            sonarId = identifier.asBytes(),
                            rssiValues = listOf(rssi),
                            rssiTimestamps = listOf(timestamp.millis),
                            timestamp = timestamp.millis,
                            duration = 60
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
