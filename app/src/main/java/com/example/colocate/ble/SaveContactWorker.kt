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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.*

class SaveContactWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val id = inputData.getString("identifier")
                ?: return@withContext Result.failure()

            // 255 is invalid for rssi
            val rssi = inputData.getInt("rssi", 255)
            if (rssi == 255) {
                return@withContext Result.failure()
            }

            try {
                val file = File(context.filesDir, EVENT_FILENAME)
                file.createNewFile()
                val moshi = Moshi.Builder().build()

                val adapter = moshi.adapter(ContactEvent::class.java)

                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK).let {
                    it.timeZone = TimeZone.getTimeZone("UTC")
                    it.format(Date())
                }

                val event = adapter.toJson(ContactEvent(id, rssi, timestamp))
                    .replace("\n", "")
                file.appendText("$event\n")

                Log.i(TAG, "event ${event}")

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