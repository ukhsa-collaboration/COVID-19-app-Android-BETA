/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.colocate.appComponent
import com.example.colocate.ble.util.isBluetoothEnabled
import com.example.colocate.di.module.AppModule
import com.example.colocate.getChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class BluetoothService : Service() {
    companion object {
        const val COLOCATE_SERVICE_ID = 1235
    }

    @Inject
    lateinit var advertise: Advertise

    @Inject
    lateinit var scan: Scanner

    @Inject
    lateinit var gatt: Gatt

    @Inject
    @Named(AppModule.DISPATCHER_MAIN)
    lateinit var coroutineDispatcher: CoroutineDispatcher

    private lateinit var coroutineScope: CoroutineScope

    private var isStarted = false

    override fun onCreate() {
        super.onCreate()
        startForeground(COLOCATE_SERVICE_ID, notification())

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, filter)
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("bluetoothReceiver onReceive")
            if (!isStarted) {
                Timber.d("bluetoothReceiver service is not started")
                return
            }

            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            Timber.d("bluetoothReceiver bluetooth adapter state: $state")
            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    Timber.d("bluetoothReceiver starting gatt and advertising")
                    gatt.start()
                    advertise.start()
                }
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                    Timber.d("bluetoothReceiver stop gatt and advertising")
                    gatt.stop()
                    advertise.stop()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?

        Timber.d("BluetoothService onStartCommand ${bluetoothManager?.adapter}")
        if (bluetoothManager?.adapter == null) {
            return START_NOT_STICKY
        }

        Timber.d("BluetoothService isStarted $isStarted")

        if (!isStarted && isBluetoothEnabled()) {
            isStarted = true
            Timber.d("BluetoothService started")
            appComponent.inject(this)
            coroutineScope = CoroutineScope(coroutineDispatcher + Job())
            Timber.d("BluetoothService start all sub-services")
            gatt.start()
            advertise.start()
            scan.start(coroutineScope)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        isStarted = false
        unregisterReceiver(bluetoothReceiver)
        stop()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stop()
    }

    private fun stop() {
        Timber.d("BluetoothService stop all sub-services")
        isStarted = false
        gatt.stop()
        scan.stop()
        advertise.stop()
        coroutineScope.cancel()
    }

    private fun isPermissionGranted() = true

    private fun notification(): Notification {

        return NotificationCompat.Builder(this, getChannel(applicationContext))
            .build()
    }
}
