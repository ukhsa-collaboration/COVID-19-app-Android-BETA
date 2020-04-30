/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.debug

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.Seconds
import uk.nhs.nhsx.sonar.android.app.MainActivity
import uk.nhs.nhsx.sonar.android.app.ble.BleEvents
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class TestViewModel @Inject constructor(
    private val context: Context,
    private val contactEventDao: ContactEventDao,
    private val eventTracker: BleEvents
) : ViewModel() {

    fun clear() {
        viewModelScope.launch {
            context.stopService(Intent(context, BluetoothService::class.java))

            contactEventDao.clearEvents()
            eventTracker.clear()
            withContext(Dispatchers.IO) {
                FirebaseInstanceId.getInstance().deleteInstanceId()
                WorkManager.getInstance(context).cancelUniqueWork(REGISTRATION_WORK)
            }

            MainActivity.start(context)
        }
    }

    fun storeEvents(activityContext: Context) {
        viewModelScope.launch {
            val events = contactEventDao.getAll()
            val text = events.joinToString("\n") {
                val eventTime = DateTime(it.timestamp)
                val rssiIntervals = it.rssiTimestamps.mapIndexed { index, timestamp ->
                    return@mapIndexed if (index == 0) 0
                    else
                        Seconds.secondsBetween(
                            DateTime(it.rssiTimestamps[index - 1]),
                            DateTime(timestamp)
                        ).seconds
                }.joinToString(":")

                "${Base64.encodeToString(it.sonarId, Base64.DEFAULT).replace(
                    "\n",
                    ""
                )},${eventTime.toUtcIsoFormat()},${it.duration},${it.rssiValues.joinToString(":")},$rssiIntervals"
            }

            val zipFile = "contact-events-exports.zip"

            activityContext.openFileOutput(zipFile, Context.MODE_PRIVATE).use {
                ZipOutputStream(it).use { zip ->
                    zip.putNextEntry(ZipEntry("contact-events.csv"))
                    zip.write(text.toByteArray())
                }
            }

            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(
                        activityContext,
                        "uk.nhs.nhsx.sonar.android.app.exports",
                        File(context.filesDir, zipFile)
                    )
                )
                type = "application/zip"
            }

            activityContext.startActivity(Intent.createChooser(sendIntent, "Export events"))
        }
    }

    fun observeConnectionEvents() = eventTracker.observeConnectionEvents()

    companion object {
        private const val REGISTRATION_WORK = "registration"
    }
}
