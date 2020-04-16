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
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule.Companion.ERROR_MARGIN
import javax.inject.Named

interface SaveContactWorker {
    fun createOrUpdateContactEvent(scope: CoroutineScope, id: ByteArray, rssi: Int, timestamp: DateTime)
    fun saveContactEvent(scope: CoroutineScope, record: Record)

    data class Record(
        val timestamp: DateTime,
        val sonarId: Identifier,
        val rssiValues: MutableList<Int> = mutableListOf(),
        val duration: Int = 0
    )
}

class DefaultSaveContactWorker(
    @Named(AppModule.DISPATCHER_IO) private val dispatcher: CoroutineDispatcher,
    @Named(ERROR_MARGIN) private val errorMargin: Int,
    private val contactEventDao: ContactEventDao
) : SaveContactWorker {

    override fun createOrUpdateContactEvent(scope: CoroutineScope, id: ByteArray, rssi: Int, timestamp: DateTime) {
        scope.launch {
            withContext(dispatcher) {
                try {
                    val contactEvent =
                        ContactEvent(
                            sonarId = id,
                            rssiValues = listOf(rssi),
                            timestamp = timestamp.millis,
                            duration = 60
                        )
                    contactEventDao.createOrUpdate(contactEvent, errorMargin)
                    Timber.i("$TAG saving event $contactEvent")
                } catch (e: Exception) {
                    Timber.e("$TAG Failed to save with exception $e")
                }
            }
        }
    }

    override fun saveContactEvent(scope: CoroutineScope, record: SaveContactWorker.Record) {
        scope.launch {
            withContext(dispatcher) {
                try {
                    val timestamp = record.timestamp.millis

                    val contactEvent =
                        ContactEvent(
                            sonarId = record.sonarId.asBytes,
                            rssiValues = record.rssiValues,
                            timestamp = timestamp,
                            duration = record.duration
                        )
                    contactEventDao.insert(contactEvent)
                    Timber.i("$TAG eventV2 $contactEvent")
                } catch (e: Exception) {
                    Timber.e("$TAG Failed to save eventV2 with exception $e")
                }
            }
        }
    }

    companion object {
        const val TAG = "SaveWorker"
    }
}
