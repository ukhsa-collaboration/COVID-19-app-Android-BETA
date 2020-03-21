/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class SaveContactWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val id = inputData.getString("identifier")
                ?: return@withContext Result.failure()
            // figure out how to average over the last few measurements
            // to compute distance
//            val rssi = inputData.getString("rssi")
//                ?: return@withContext Result.failure()

            val event = ContactEvent(id)

            try {
                val file = File(context.filesDir, EVENT_FILENAME)

                val moshi = Moshi.Builder()
//                    .add(UUID::class.java, object : JsonAdapter<UUID>() {
//                        override fun fromJson(reader: JsonReader): UUID? =
//                            UUID.fromString(reader.nextString())
//                        override fun toJson(writer: JsonWriter, value: UUID?) {
//                            writer.value(value.toString())
//                        }
//                    })
                    .build()

                val adapter: JsonAdapter<List<ContactEvent>> = moshi.adapter(
                    Types.newParameterizedType(MutableList::class.java, ContactEvent::class.java)
                )

                val previous = if (file.createNewFile() || file.readBytes().isEmpty()) {
                    mutableListOf()
                } else {
                    adapter.fromJson(file.readText()) ?: listOf()
                }
                val events = previous.plus(event)

                Log.i(TAG, "#events ${events.size} last event ${events.last()}")
                file.writeText(adapter.toJson(events))

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save with exception $e")
                Result.failure()
            }
        }


    companion object {
        const val EVENT_FILENAME = "events.json"
        const val TAG = "SaveWorker"
    }
}