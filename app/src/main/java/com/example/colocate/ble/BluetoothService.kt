/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.colocate.ColocateApplication
import com.example.colocate.ble.util.isBluetoothEnabled
import com.example.colocate.di.module.AppModule
import com.example.colocate.getChannel
import com.example.colocate.hasLocationPermission
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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

    object Lock

    override fun onCreate() {
        super.onCreate()
        (applicationContext as ColocateApplication).applicationComponent.inject(this)
        coroutineScope = CoroutineScope(coroutineDispatcher + Job())
        startForeground(COLOCATE_SERVICE_ID, notification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?

        if (bluetoothManager?.adapter == null || !isPermissionGranted()) {
            return START_NOT_STICKY
        }

        synchronized(Lock) {
            if (!isStarted and isBluetoothEnabled() and hasLocationPermission(applicationContext)) {
                gatt.start()
                advertise.start()
                scan.start(coroutineScope)
                isStarted = true
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stop()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stop()
    }

    private fun stop() {
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
