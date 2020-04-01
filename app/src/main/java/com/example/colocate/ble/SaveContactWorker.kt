/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import com.example.colocate.di.module.AppModule
import com.example.colocate.persistence.ContactEvent
import com.example.colocate.persistence.ContactEventDao
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
    private val dateProvider: () -> Date = { Date() }
) {
    fun saveContactEvent(scope: CoroutineScope, id: String, rssi: Int) {
        scope.launch {
            withContext(dispatcher) {
                try {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK).let {
                        it.timeZone = TimeZone.getTimeZone("UTC")
                        it.format(dateProvider())
                    }

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

    companion object {
        const val TAG = "SaveWorker"
    }
}
