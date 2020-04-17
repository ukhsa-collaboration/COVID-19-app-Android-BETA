/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.polidea.rxandroidble2.RxBleClient
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ServiceRestarterBroadcastReceiver
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.util.hideBluetoothIsDisabledNotification
import uk.nhs.nhsx.sonar.android.app.util.hideLocationIsDisabledNotification
import uk.nhs.nhsx.sonar.android.app.util.notificationBuilder
import uk.nhs.nhsx.sonar.android.app.util.showBluetoothIsDisabledNotification
import uk.nhs.nhsx.sonar.android.app.util.showLocationIsDisabledNotification
import javax.inject.Inject
import javax.inject.Named

class BluetoothService : Service() {
    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1235

        fun start(context: Context) =
            ContextCompat.startForegroundService(
                context,
                Intent(context, BluetoothService::class.java)
            )
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
    private val locationProviderChangedReceiver = LocationProviderChangedReceiver()

    data class CombinedStatus(
        val isBleClientInReadyState: Boolean,
        val isBluetoothEnabled: Boolean,
        val isLocationEnabled: Boolean
    )

    override fun onCreate() {
        super.onCreate()
        startForeground(FOREGROUND_NOTIFICATION_ID, notificationBuilder().build())

        val bleClient = appComponent.provideRxBleClient()
        stateChangeDisposable = Observable.combineLatest(
            bleClient.observeStateChanges().startWith(bleClient.state),
            locationProviderChangedReceiver.getLocationStatus(),
            BiFunction<RxBleClient.State, Boolean, CombinedStatus> { bleClientState, isLocationEnabled ->
                val isBleClientInReadyState = bleClientState == RxBleClient.State.READY
                Timber.d("RxClientBleState = $bleClientState}")
                val isBluetoothEnabled = isBluetoothEnabled()
                CombinedStatus(isBleClientInReadyState, isBluetoothEnabled, isLocationEnabled)
            })
            .subscribe { status ->
                Timber.d("Combined state $status")
                if (status.isLocationEnabled) {
                    hideLocationIsDisabledNotification(this)
                } else {
                    showLocationIsDisabledNotification(this)
                }

                if (status.isBluetoothEnabled) {
                    hideBluetoothIsDisabledNotification(this)
                } else {
                    stopGattAndAdvertise()
                    showBluetoothIsDisabledNotification(this)
                }

                if (status.isBleClientInReadyState) {
                    if (!isInjected) {
                        appComponent.inject(this)
                        isInjected = true
                    }
                    startGattAndAdvertise()
                    startScan()
                }
            }

        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(locationProviderChangedReceiver, filter)
        locationProviderChangedReceiver.onCreate(this)
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
        sendBroadcastToRestartService()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        sendBroadcastToRestartService()
    }

    private fun isBluetoothEnabled() =
        BluetoothAdapter.getDefaultAdapter().isEnabled

    private fun sendBroadcastToRestartService() {
        val broadcastIntent = Intent(this, ServiceRestarterBroadcastReceiver::class.java).apply {
            action = ServiceRestarterBroadcastReceiver.ACTION_RESTART_BLUETOOTH_SERVICE
        }
        sendBroadcast(broadcastIntent)
    }

    private fun startScan() {
        Timber.d("startScan isScanRunning = $isScanRunning")
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
        Timber.d("startGattAndAdvertise areGattAndAdvertiseRunning = $areGattAndAdvertiseRunning")
        if (!areGattAndAdvertiseRunning) {
            areGattAndAdvertiseRunning = true
            gatt.start()
            advertise.start()
        }
    }

    private fun stopGattAndAdvertise() {
        Timber.d("stopGattAndAdvertise areGattAndAdvertiseRunning = $areGattAndAdvertiseRunning")
        if (areGattAndAdvertiseRunning) {
            areGattAndAdvertiseRunning = false
            gatt.stop()
            advertise.stop()
        }
    }

    private fun stopScan() {
        Timber.d("stopScan isScanRunning = $isScanRunning")
        if (isScanRunning) {
            isScanRunning = false
            coroutineScope.cancel()
            scan.stop()
        }
    }

    inner class LocationProviderChangedReceiver : BroadcastReceiver() {

        private var isGpsEnabled: Boolean = false
        private var isNetworkEnabled: Boolean = false

        private val subject = BehaviorSubject.create<Boolean>()

        fun getLocationStatus(): Observable<Boolean> {
            return subject.distinctUntilChanged()
        }

        fun onCreate(context: Context) {
            checkStatus(context)
        }

        override fun onReceive(context: Context, intent: Intent) {
            intent.action?.let { act ->
                if (act.matches("android.location.PROVIDERS_CHANGED".toRegex())) {
                    checkStatus(context)
                }
            }
        }

        private fun checkStatus(context: Context) {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            Timber.i("Location Providers changed, is GPS Enabled: $isGpsEnabled is network enabled = $isNetworkEnabled")

            val isEnabled = isGpsEnabled || isNetworkEnabled
            subject.onNext(isEnabled)
        }
    }
}
