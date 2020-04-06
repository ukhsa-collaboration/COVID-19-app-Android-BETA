/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.location.LocationManager
import android.os.IBinder
import androidx.core.location.LocationManagerCompat
import com.example.colocate.appComponent
import com.example.colocate.di.module.AppModule
import com.example.colocate.util.hideBluetoothIsDisabledNotification
import com.example.colocate.util.hideLocationIsDisabledNotification
import com.example.colocate.util.notificationBuilder
import com.example.colocate.util.showBluetoothIsDisabledNotification
import com.example.colocate.util.showLocationIsDisabledNotification
import com.polidea.rxandroidble2.RxBleClient
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class BluetoothService : Service() {
    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1235
    }

    private var stateChangeDisposable: Disposable? = null

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

    private var isInjected = false
    private var areGattAndAdvertiseRunning = false
    private var isScanRunning = false

    override fun onCreate() {
        super.onCreate()
        startForeground(FOREGROUND_NOTIFICATION_ID, notificationBuilder().build())

        val bleClient = appComponent.provideRxBleClient()
        stateChangeDisposable = bleClient
            .observeStateChanges()
            .startWith(bleClient.state)
            .subscribe { state ->
                when (state) {
                    RxBleClient.State.BLUETOOTH_NOT_AVAILABLE -> {
                    }
                    RxBleClient.State.LOCATION_PERMISSION_NOT_GRANTED -> {
                    }
                    RxBleClient.State.BLUETOOTH_NOT_ENABLED -> {
                        Timber.d("bluetoothReceiver stop gatt and advertising")
                        stopGattAndAdvertise()
                        showBluetoothIsDisabledNotification(this)

                        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                        if (LocationManagerCompat.isLocationEnabled(locationManager)) {
                            hideLocationIsDisabledNotification(this)
                        }
                    }
                    RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED -> {
                        showLocationIsDisabledNotification(this)
                        val defaultAdapter = BluetoothAdapter.getDefaultAdapter()
                        if (defaultAdapter != null) {
                            val state = defaultAdapter.state
                            if (state == BluetoothAdapter.STATE_TURNING_ON || state == BluetoothAdapter.STATE_ON) {
                                hideBluetoothIsDisabledNotification(this)
                            }
                        }
                    }
                    RxBleClient.State.READY -> {
                        if (!isInjected) {
                            appComponent.inject(this)
                            isInjected = true
                        }
                        startGattAndAdvertise()
                        startScan()
                        hideBluetoothIsDisabledNotification(this)
                        hideLocationIsDisabledNotification(this)
                    }
                }
            }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?
        Timber.d("BluetoothService onStartCommand ${bluetoothManager?.adapter}")
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.d("BluetoothService onDestroy")
        stopSubServices()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSubServices()
    }

    private fun startScan() {
        if (!isScanRunning) {
            isScanRunning = true
            coroutineScope = CoroutineScope(coroutineDispatcher + Job())
            scan.start(coroutineScope)
        }
    }

    private fun stopSubServices() {
        Timber.d("BluetoothService stop all sub-services")
        stopGattAndAdvertise()
        stopScan()
        stateChangeDisposable?.dispose()
    }

    private fun startGattAndAdvertise() {
        if (!areGattAndAdvertiseRunning) {
            areGattAndAdvertiseRunning = true
            Timber.d("BluetoothService startGattAndAdvertise")
            gatt.start()
            advertise.start()
        }
    }

    private fun stopGattAndAdvertise() {
        if (areGattAndAdvertiseRunning) {
            areGattAndAdvertiseRunning = false
            gatt.stop()
            advertise.stop()
        }
    }

    private fun stopScan() {
        if (isScanRunning) {
            isScanRunning = false
            coroutineScope.cancel()
            scan.stop()
        }
    }
}
