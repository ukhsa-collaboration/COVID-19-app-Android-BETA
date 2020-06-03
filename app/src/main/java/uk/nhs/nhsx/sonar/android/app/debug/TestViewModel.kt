/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.debug

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.nhs.nhsx.sonar.android.app.MainActivity
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.ble.DebugBleEventTracker
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.contactevents.timestampsToIntervals
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramStorage
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager.Companion.REGISTRATION_WORK
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class TestViewModel @Inject constructor(
    private val context: Context,
    private val contactEventDao: ContactEventDao,
    private val eventTracker: DebugBleEventTracker,
    private val cryptogramStorage: CryptogramStorage
) : ViewModel() {
    private val cryptogramLiveData = MutableLiveData<Cryptogram>()
    private var cryptogramChecker: Job? = null

    fun clear() {
        viewModelScope.launch {
            context.stopService(Intent(context, BluetoothService::class.java))

            contactEventDao.clearEvents()
            eventTracker.clear()
            withContext(Dispatchers.IO) {
                FirebaseInstanceId.getInstance().deleteInstanceId()
                WorkManager.getInstance(context).let {
                    it.cancelUniqueWork(REGISTRATION_WORK)
                }
            }

            MainActivity.start(context)
        }
    }

    fun storeEvents(activityContext: Context) {
        viewModelScope.launch {
            val contactEvents = contactEventDao.getAll()
                .joinToString("\n") {
                    val eventTime = DateTime(it.timestamp, DateTimeZone.UTC)
                    val rssiIntervals = it.rssiTimestamps
                        .timestampsToIntervals()
                        .joinToString(":")

                    val remoteContactId = Base64.encodeToString(it.sonarId, Base64.DEFAULT).replace(
                        "\n",
                        ""
                    )
                    "$remoteContactId,${eventTime.toUtcIsoFormat()},${it.duration},${it.rssiValues.joinToString(
                        ":"
                    )},$rssiIntervals"
                }

            val errors = eventTracker.getErrors().joinToString("\n") {
                "${it.timestamp},${it.macAddress},${it.error}"
            }

            val zipFile = "contact-events-exports.zip"
            activityContext.openFileOutput(zipFile, Context.MODE_PRIVATE).use {
                ZipOutputStream(it).use { zip ->
                    zip.putNextEntry(ZipEntry("contact-events.csv"))
                    zip.write(contactEvents.toByteArray())
                    zip.putNextEntry(ZipEntry("errors.csv"))
                    zip.write(errors.toByteArray())
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

    fun observeContactEvents() = eventTracker.observeContactEvents()

    override fun onCleared() {
        super.onCleared()
        cryptogramChecker?.cancel()
    }

    fun observeCryptogram(): LiveData<Cryptogram> {
        // Not the cleanest, it'd be nicer if the provider could update a LiveData property,
        // but somehow this ViewModel gets a different instance than the GattServer.
        viewModelScope.launch {
            while (isActive) {
                val storedCryptogram = cryptogramStorage.get().second
                if (storedCryptogram != null) cryptogramLiveData.postValue(storedCryptogram)
                delay(5_000)
            }
        }
        return cryptogramLiveData
    }
}
