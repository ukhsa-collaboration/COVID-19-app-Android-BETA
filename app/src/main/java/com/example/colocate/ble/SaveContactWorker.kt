/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import com.example.colocate.di.module.AppModule
import com.example.colocate.persistence.ContactEvent
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ContactEventV2
import com.example.colocate.persistence.ContactEventV2Dao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Named

class SaveContactWorker(
    @Named(AppModule.DISPATCHER_IO) private val dispatcher: CoroutineDispatcher,
    private val contactEventDao: ContactEventDao,
    private val contactEventV2Dao: ContactEventV2Dao,
    private val dateProvider: () -> Date = { Date() }
) {
    fun saveContactEvent(scope: CoroutineScope, id: String, rssi: Int) {
        scope.launch {
            withContext(dispatcher) {
                try {
                    val timestamp = formatTimestamp(dateProvider())

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

    fun saveContactEventV2(scope: CoroutineScope, record: Record) {
        scope.launch {
            withContext(dispatcher) {
                try {
                    val timestamp = formatTimestamp(record.timestamp)

                    val contactEvent =
                        ContactEventV2(
                            sonarId = record.sonarId.asString,
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

    private fun formatTimestamp(date: Date): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK).let {
            it.timeZone = TimeZone.getTimeZone("UTC")
            it.format(date)
        }
        return timestamp
    }

    companion object {
        const val TAG = "SaveWorker"
    }

    data class Record(
        val timestamp: Date,
        val sonarId: Identifier,
        val rssiValues: MutableList<Int> = mutableListOf(),
        val duration: Long = 0
    )
}
