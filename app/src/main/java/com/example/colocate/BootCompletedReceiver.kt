package com.example.colocate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.colocate.ble.BluetoothService
import com.example.colocate.persistence.ResidentIdProvider
import timber.log.Timber
import javax.inject.Inject

class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var residentIdProvider: ResidentIdProvider

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("CoLocate onReceive: $intent")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            (context.applicationContext as ColocateApplication).appComponent.inject(this)

            Timber.d("CoLocate onReceive hasProperResidentId: ${residentIdProvider.hasProperResidentId()}")
            if (residentIdProvider.hasProperResidentId()) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, BluetoothService::class.java)
                )
            }
        }
    }
}
