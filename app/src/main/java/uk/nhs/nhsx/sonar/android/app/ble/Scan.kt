/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.os.ParcelUuid
import com.polidea.rxandroidble2.LogConstants
import com.polidea.rxandroidble2.LogOptions
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import javax.inject.Inject
import javax.inject.Named

class Scan @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val saveContactWorker: SaveContactWorker,
    private val bleEvents: BleEvents,
    private val currentTimestampProvider: () -> DateTime = { DateTime.now(DateTimeZone.UTC) },
    @Named(BluetoothModule.ENCRYPT_SONAR_ID)
    private val encryptSonarId: Boolean,
    @Named(BluetoothModule.SCAN_INTERVAL_LENGTH)
    private val scanIntervalLength: Int
) : Scanner {

    private var running = true
    private var devices: MutableList<ScanResult> = mutableListOf()
    private val connections: MutableList<Disposable?> = mutableListOf()

    private val coLocateServiceUuidFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
        .build()

    private var scanDisposable: Disposable? = null

    /*
     When the iPhone app goes into the background iOS changes how services are advertised:
  
         1) The service uuid is now null
         2) The information to identify the service is encoded into the manufacturing data in a
         unspecified/undocumented way.
  
        The below filter is based on observation of the advertising packets produced by an iPhone running
        the app in the background.
       */
    private val coLocateBackgroundedIPhoneFilter = ScanFilter.Builder()
        .setServiceUuid(null)
        .setManufacturerData(
            76,
            byteArrayOf(
                0x01, // 0
                0x00, // 1
                0x00, // 2
                0x00, // 3
                0x00, // 4
                0x00, // 5
                0x00, // 6
                0x00, // 7
                0x00, // 8
                0x00, // 9
                0x40, // 10
                0x00, // 11
                0x00, // 12
                0x00, // 13
                0x00, // 14
                0x00, // 15
                0x00 // 16
            )
        )
        .build()

    private val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()

    override fun start(coroutineScope: CoroutineScope) {
        val logOptions = LogOptions.Builder()
            .setLogLevel(LogConstants.DEBUG)
            .setLogger { level, tag, msg -> Timber.tag(tag).log(level, msg) }
            .build()
        RxBleClient.updateLogOptions(logOptions)
        coroutineScope.launch {
            while (running) {
                Timber.d("scan - Starting")
                scanDisposable = scan()
                delay(scanIntervalLength.toLong() * 1_000)

                Timber.d("scan - Stopping")
                scanDisposable?.dispose()
                scanDisposable = null

                // Some devices are unable to connect while a scan is running
                // or just after it finished
                delay(1_000)

                devices.distinctBy { it.bleDevice }.map {
                    Timber.d("scan - Connecting to $it")
                    connectToDevice(it, coroutineScope)
                }

                connections.map { it?.dispose() }
                connections.clear()
                devices.clear()
            }
        }
    }

    private fun scan(): Disposable? {
        return rxBleClient
            .scanBleDevices(
                settings,
                coLocateBackgroundedIPhoneFilter,
                coLocateServiceUuidFilter
            )
            .subscribe(
                {
                    Timber.d("Scan found = ${it.bleDevice}")
                    devices.add(it)
                },
                ::onConnectionError
            )
    }

    override fun stop() {
        running = false
        scanDisposable?.dispose()
        scanDisposable = null
    }

    private fun connectToDevice(
        scanResult: ScanResult,
        coroutineScope: CoroutineScope
    ) {
        val macAddress = scanResult.bleDevice.macAddress

        Timber.d("Connecting to $macAddress")
        scanResult
            .bleDevice
            .establishConnection(false)
            .flatMapSingle { connection ->
                negotiatieMTU(connection)
            }
            .flatMapSingle { connection ->
                read(connection, coroutineScope)
            }
            .doOnSubscribe {
                connections.add(it)
            }
            .take(1)
            .blockingSubscribe(
                { event ->
                    onReadSuccess(event)
                },
                { e ->
                    Timber.e("failed reading from $macAddress - $e")
                }
            )
    }

    private fun negotiatieMTU(connection: RxBleConnection): Single<RxBleConnection> {
        // the overhead appears to be 2 bytes
        return connection.requestMtu(2 + Cryptogram.SIZE)
            .doOnSubscribe { Timber.i("Negotiating MTU started") }
            .doOnError { e: Throwable? ->
                Timber.e("Failed to negotiate MTU: $e")
                Observable.error<Throwable?>(e)
            }
            .doOnSuccess { Timber.i("Negotiated MTU: $it") }
            .ignoreElement()
            .andThen(Single.just(connection))
    }

    private fun read(connection: RxBleConnection, scope: CoroutineScope): Single<Event> {
        return Single.zip(
            connection.readCharacteristic(DEVICE_CHARACTERISTIC_UUID),
            connection.readRssi(),
            BiFunction<ByteArray, Int, Event> { characteristicValue, rssi ->
                Event(characteristicValue, rssi, scope, currentTimestampProvider())
            }
        )
    }

    private fun onConnectionError(e: Throwable) {
        bleEvents.scanFailureEvent()
        Timber.e("Connection failed with: $e")
    }

    private fun onReadSuccess(event: Event) {
        if (!encryptSonarId) {
            bleEvents.connectedDeviceEvent(
                Identifier.fromBytes(event.identifier).asString,
                listOf(event.rssi)
            )
        } else {
            bleEvents.connectedDeviceEvent(
                Cryptogram.fromBytes(event.identifier).asString(),
                listOf(event.rssi)
            )
        }

        saveContactWorker.createOrUpdateContactEvent(
            event.scope,
            event.identifier,
            event.rssi,
            event.timestamp
        )
    }

    // We're really just using this as a bundle, and never comparing different events.
    @Suppress("ArrayInDataClass")
    private data class Event(
        val identifier: ByteArray,
        val rssi: Int,
        val scope: CoroutineScope,
        val timestamp: DateTime
    )
}
