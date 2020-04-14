/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEvent
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventV2
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventV2Dao
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import javax.inject.Named

interface SaveContactWorker {
    fun saveContactEvent(scope: CoroutineScope, id: String, rssi: Int)
    fun saveContactEventV2(scope: CoroutineScope, record: Record)

    data class Record(
        val timestamp: DateTime,
        val sonarId: Identifier,
        val rssiValues: MutableList<Int> = mutableListOf(),
        val duration: Long = 0
    )
}

class DefaultSaveContactWorker(
    @Named(AppModule.DISPATCHER_IO) private val dispatcher: CoroutineDispatcher,
    private val contactEventDao: ContactEventDao,
    private val contactEventV2Dao: ContactEventV2Dao,
    private val dateProvider: () -> DateTime = { DateTime.now(DateTimeZone.UTC) }
) : SaveContactWorker {

    override fun saveContactEvent(scope: CoroutineScope, id: String, rssi: Int) {
        scope.launch {
            withContext(dispatcher) {
                try {
                    val timestamp = dateProvider().toUtcIsoFormat()

                    val contactEvent =
                        ContactEvent(
                            remoteContactId = id,
                            rssi = rssi,
                            timestamp = timestamp
                        )
                    contactEventDao.insert(contactEvent)
                    Timber.i("$TAG event $contactEvent")
                } catch (e: Exception) {
                    Timber.e("$TAG Failed to save with exception $e")
                }
            }
        }
    }

    override fun saveContactEventV2(scope: CoroutineScope, record: SaveContactWorker.Record) {
        scope.launch {
            withContext(dispatcher) {
                try {
                    val timestamp = record.timestamp.toUtcIsoFormat()

                    val contactEvent =
                        ContactEventV2(
                            sonarId = record.sonarId.asBytes,
                            rssiValues = record.rssiValues,
                            timestamp = timestamp,
                            duration = record.duration
                        )
                    contactEventV2Dao.insert(contactEvent)
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
