/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.os.ParcelUuid
import android.util.Base64
import com.polidea.rxandroidble2.LogConstants
import com.polidea.rxandroidble2.LogOptions
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.BuildConfig
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import javax.inject.Inject
import javax.inject.Named

class Scanner @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val saveContactWorker: SaveContactWorker,
    private val eventEmitter: BleEventEmitter,
    private val currentTimestampProvider: () -> DateTime = { DateTime.now(DateTimeZone.UTC) },
    @Named(BluetoothModule.SCAN_INTERVAL_LENGTH)
    private val scanIntervalLength: Int,
    base64Decoder: (String) -> ByteArray = { Base64.decode(it, Base64.DEFAULT) }
) {

    private var devices: MutableList<Pair<ScanResult, Int>> = mutableListOf()

    private val sonarServiceUuidFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(SONAR_SERVICE_UUID))
        .build()

    private var scanDisposable: Disposable? = null
    private var scanJob: Job? = null
    private val appleManufacturerId = 76
    private val encodedBackgroundIosServiceUuid: ByteArray =
        base64Decoder(BuildConfig.SONAR_ENCODED_BACKGROUND_IOS_SERVICE_UUID)

    /*
     When the iPhone app goes into the background iOS changes how services are advertised:
  
         1) The service uuid is now null
         2) The information to identify the service is encoded into the manufacturing data in a
         unspecified/undocumented way.
  
        The below filter is based on observation of the advertising packets produced by an iPhone running
        the app in the background.
       */
    private val sonarBackgroundedIPhoneFilter = ScanFilter.Builder()
        .setServiceUuid(null)
        .setManufacturerData(
            appleManufacturerId,
            encodedBackgroundIosServiceUuid
        )
        .build()

    private val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()

    fun start(coroutineScope: CoroutineScope) {
        val logOptions = LogOptions.Builder()
            .setLogLevel(LogConstants.DEBUG)
            .setLogger { level, tag, msg -> Timber.tag(tag).log(level, msg) }
            .build()
        RxBleClient.updateLogOptions(logOptions)

        scanJob = coroutineScope.launch {
            while (isActive) {

                Timber.d("scan - Starting")
                scanDisposable = scan()

                var attempts = 0
                while (attempts++ < 10 && devices.isEmpty()) {
                    if (!isActive) {
                        disposeScanDisposable()
                        return@launch
                    }
                    delay(scanIntervalLength.toLong() * 1_000)
                }

                Timber.d("scan - Stopping")
                disposeScanDisposable()

                if (!isActive) return@launch

                // Some devices are unable to connect while a scan is running
                // or just after it finished
                delay(1_000)

                devices.distinctBy { it.first.bleDevice }.map {
                    Timber.d("scan - Connecting to $it")
                    connectToDevice(it.first, it.second, coroutineScope)
                }

                devices.clear()
            }
        }
    }

    fun stop() {
        disposeScanDisposable()
        scanJob?.cancel()
    }

    private fun disposeScanDisposable() {
        scanDisposable?.dispose()
        scanDisposable = null
    }

    private fun scan(): Disposable? =
        rxBleClient
            .scanBleDevices(
                settings,
                sonarBackgroundedIPhoneFilter,
                sonarServiceUuidFilter
            )
            .subscribe(
                {
                    Timber.d("Scan found = ${it.bleDevice}")
                    devices.add(Pair(it, it.scanRecord.txPowerLevel))
                },
                ::onConnectionError
            )

    private fun connectToDevice(
        scanResult: ScanResult,
        txPowerAdvertised: Int,
        coroutineScope: CoroutineScope
    ) {
        val macAddress = scanResult.bleDevice.macAddress

        val compositeDisposable = CompositeDisposable()

        Timber.d("Connecting to $macAddress")
        scanResult
            .bleDevice
            .establishConnection(false)
            .flatMapSingle { connection ->
                negotiateMTU(connection)
            }
            .flatMapSingle { connection ->
                read(connection, txPowerAdvertised, coroutineScope)
            }
            .doOnSubscribe {
                compositeDisposable.add(it)
            }
            .take(1)
            .blockingSubscribe(
                { event ->
                    compositeDisposable.dispose()
                    storeEvent(event)
                },
                { e ->
                    compositeDisposable.dispose()
                    Timber.e("failed reading from $macAddress - $e")
                }
            )
    }

    private fun negotiateMTU(connection: RxBleConnection): Single<RxBleConnection> {
        // the overhead appears to be 2 bytes
        return connection.requestMtu(2 + BluetoothIdentifier.SIZE)
            .doOnSubscribe { Timber.i("Negotiating MTU started") }
            .doOnError { e: Throwable? ->
                Timber.e("Failed to negotiate MTU: $e")
                Observable.error<Throwable?>(e)
            }
            .doOnSuccess { Timber.i("Negotiated MTU: $it") }
            .ignoreElement()
            .andThen(Single.just(connection))
    }

    private fun read(
        connection: RxBleConnection,
        txPower: Int,
        scope: CoroutineScope
    ): Single<Event> =
        Single.zip(
            connection.readCharacteristic(SONAR_IDENTITY_CHARACTERISTIC_UUID),
            connection.readRssi(),
            BiFunction<ByteArray, Int, Event> { characteristicValue, rssi ->
                Event(characteristicValue, rssi, txPower, scope, currentTimestampProvider())
            }
        )

    private fun onConnectionError(e: Throwable) {
        Timber.e("Connection failed with: $e")
    }

    private fun storeEvent(event: Event) {
        Timber.d("Event $event")
        eventEmitter.connectedDeviceEvent(
            event.identifier,
            listOf(event.rssi),
            event.txPower
        )

        saveContactWorker.createOrUpdateContactEvent(
            event.scope,
            event.identifier,
            event.rssi,
            event.timestamp,
            event.txPower
        )
    }

    // We're really just using this as a bundle, and never comparing different events.
    @Suppress("ArrayInDataClass")
    private data class Event(
        val identifier: ByteArray,
        val rssi: Int,
        val txPower: Int,
        val scope: CoroutineScope,
        val timestamp: DateTime
    )
}
