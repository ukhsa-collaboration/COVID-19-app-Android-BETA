package com.example.colocate.debug

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.colocate.ble.BleEvents
import com.example.colocate.persistence.ContactEventV2Dao
import com.google.firebase.iid.FirebaseInstanceId
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TestViewModel(
    private val context: Context,
    private val contactEventDao: ContactEventV2Dao,
    private val eventTracker: BleEvents
) : ViewModel() {

    fun clear() {
        viewModelScope.launch {
            contactEventDao.clearEvents()
            eventTracker.clear()
            withContext(Dispatchers.IO) {
                FirebaseInstanceId.getInstance().deleteInstanceId()
            }
            ProcessPhoenix.triggerRebirth(context)
        }
    }

    fun storeEvents() {
        viewModelScope.launch {
            val events = contactEventDao.getAll()
            val text = events.joinToString("\n") {
                "${it.sonarId},${it.timestamp},${it.duration},${it.rssiValues.joinToString(":")}"
            }

            val zipFile = "contact-events-exports.zip"

            context.openFileOutput(zipFile, Context.MODE_PRIVATE).use {
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
                        context,
                        "com.example.colocate.exports",
                        File(context.filesDir, zipFile)
                    )
                )
                type = "application/zip"
            }

            context.startActivity(Intent.createChooser(sendIntent, "Export events"))
        }
    }

    fun observeConnectionEvents() = eventTracker.observeConnectionEvents()
}

class TestViewModelFactory(
    private val context: Context,
    private val contractEventDao: ContactEventV2Dao,
    private val eventTracker: BleEvents
) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TestViewModel(
            context,
            contractEventDao,
            eventTracker
        ) as T
    }
}
