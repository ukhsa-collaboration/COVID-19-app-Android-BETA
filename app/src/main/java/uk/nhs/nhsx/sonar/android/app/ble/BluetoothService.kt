/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.polidea.rxandroidble2.RxBleClient
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ServiceRestarterBroadcastReceiver
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothStatusSubscriptionHandler.CombinedStatus
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothStatusSubscriptionHandler.Delegate
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.notifications.BluetoothNotificationHelper
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationChannels.Channel.ForegroundService
import uk.nhs.nhsx.sonar.android.app.notifications.notificationBuilder
import javax.inject.Inject
import javax.inject.Named

class BluetoothService : Service(), Delegate {
    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1235

        fun start(context: Context) =
            ContextCompat.startForegroundService(
                context,
                Intent(context, BluetoothService::class.java)
            )
    }

    @Inject
    lateinit var advertise: Advertise

    @Inject
    lateinit var scanner: Scanner

    @Inject
    lateinit var gattServer: GattServer

    @Inject
    @Named(AppModule.DISPATCHER_DEFAULT)
    lateinit var coroutineDispatcher: CoroutineDispatcher

    private lateinit var locationProviderChangedReceiver: LocationProviderChangedReceiver
    private lateinit var scanScope: CoroutineScope
    private lateinit var gattScope: CoroutineScope

    private var stateChangeDisposable: Disposable? = null
    private var isInjected = false
    private var areGattAndAdvertiseRunning = false
    private var isScanRunning = false

    override fun onCreate() {
        super.onCreate()
        startForeground(FOREGROUND_NOTIFICATION_ID, notificationBuilder(ForegroundService).build())

        val bleClient = appComponent.rxBleClient()
        val notificationHelper = BluetoothNotificationHelper(this)
        val subscriptionStatusHandler = BluetoothStatusSubscriptionHandler(this, notificationHelper)

        val locationHelper = appComponent.locationHelper()
        locationProviderChangedReceiver = LocationProviderChangedReceiver(locationHelper)

        val bleClientStates = bleClient.observeStateChanges().startWith(bleClient.state)
        val locationPermissionsStates = locationProviderChangedReceiver.getLocationStatus()

        stateChangeDisposable = Observable
            .combineLatest(bleClientStates, locationPermissionsStates, combineStates)
            .subscribe { status ->
                Timber.d("Combined state $status")
                subscriptionStatusHandler.handle(status)
            }

        locationProviderChangedReceiver.register(this)
    }

    private val combineStates: BiFunction<RxBleClient.State, Boolean, CombinedStatus> =
        BiFunction { bleClientState, isLocationEnabled ->
            Timber.d("RxClientBleState = $bleClientState}")

            CombinedStatus(
                bleClientState == RxBleClient.State.READY,
                isBluetoothEnabled(),
                isLocationEnabled
            )
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?
        Timber.d("BluetoothService onStartCommand ${bluetoothManager?.adapter}")
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.d("BluetoothService onDestroy")
        unregisterReceiver(locationProviderChangedReceiver)
        stopSubServices()
        ServiceRestarterBroadcastReceiver.sendBroadcast(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        ServiceRestarterBroadcastReceiver.sendBroadcast(this)
    }

    private fun isBluetoothEnabled() =
        BluetoothAdapter.getDefaultAdapter().isEnabled

    private fun injectIfNecessary() {
        if (!isInjected) {
            appComponent.inject(this)
            isInjected = true
        }
    }

    override fun startScan() {
        injectIfNecessary()

        Timber.d("startScan isScanRunning = $isScanRunning")
        if (!isScanRunning) {
            isScanRunning = true
            scanScope = CoroutineScope(coroutineDispatcher + Job())
            scanner.start(scanScope)
        }
    }

    private fun stopSubServices() {
        Timber.d("BluetoothService stop all sub-services")
        stopGattAndAdvertise()
        stopScanner()
        stateChangeDisposable?.dispose()
    }

    override fun startGattAndAdvertise() {
        injectIfNecessary()

        Timber.d("startGattAndAdvertise areGattAndAdvertiseRunning = $areGattAndAdvertiseRunning")
        if (!areGattAndAdvertiseRunning) {
            areGattAndAdvertiseRunning = true
            gattScope = CoroutineScope(coroutineDispatcher + Job())
            gattServer.start(gattScope)
            advertise.start()
        }
    }

    override fun stopGattAndAdvertise() {
        Timber.d("stopGattAndAdvertise areGattAndAdvertiseRunning = $areGattAndAdvertiseRunning")
        if (areGattAndAdvertiseRunning) {
            areGattAndAdvertiseRunning = false
            gattScope.cancel()
            gattServer.stop()
            advertise.stop()
        }
    }

    override fun stopScanner() {
        Timber.d("stopScan isScanRunning = $isScanRunning")
        if (isScanRunning) {
            isScanRunning = false
            scanScope.cancel()
            scanner.stop()
        }
    }
}
