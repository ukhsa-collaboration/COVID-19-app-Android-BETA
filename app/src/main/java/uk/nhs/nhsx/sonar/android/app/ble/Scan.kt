/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.os.ParcelUuid
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class Scan @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val saveContactWorker: SaveContactWorker,
    private val bleEvents: BleEvents,
    private val currentTimestampeProvider: () -> DateTime = { DateTime.now(DateTimeZone.UTC) }
) : Scanner {

    private val coLocateServiceUuidFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
        .build()

    private val connectedDevices = ConcurrentHashMap<String, String>()
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
        coroutineScope.launch {
            Timber.d("scan - Kicking off delay")
            delay(2 * 60_000)
            Timber.d("scan - Restarting")
            stop()
            delay(10_000)
            start(this)
        }

        scanDisposable = rxBleClient
            .scanBleDevices(
                settings,
                coLocateBackgroundedIPhoneFilter,
                coLocateServiceUuidFilter
            )
            .subscribe(
                {
                    Timber.d("Scan found = ${it.bleDevice}")
                    connectToDevice(it, coroutineScope)
                },
                ::onConnectionError
            )
    }

    override fun stop() {
        scanDisposable?.dispose()
        scanDisposable = null
    }

    private fun connectToDevice(scanResult: ScanResult, coroutineScope: CoroutineScope) {
        val macAddress = scanResult.bleDevice.macAddress
        Timber.d("Connectiing to $macAddress")
        var connectionDisposable: Disposable? = null
        scanResult
            .bleDevice
            .establishConnection(false)
            .doFinally {
                Timber.d("Disconnecting from $connectionDisposable")
                connectionDisposable?.dispose()
                connectionDisposable = null
                connectedDevices.remove(macAddress)
            }
            .flatMapSingle {
                Timber.d("Reading from - MAC:$macAddress")
                read(it, coroutineScope)
            }
            .subscribe(
                { event ->
                    onReadSuccess(event, macAddress)
                },
                { e ->
                    Timber.d("failed reading from $macAddress")
                    onReadError(e)
                }
            ).let { connectionDisposable = it }
    }

    private fun read(connection: RxBleConnection, scope: CoroutineScope): Single<Event> {
        return Single.zip(
            connection.readCharacteristic(DEVICE_CHARACTERISTIC_UUID),
            connection.readRssi(),
            BiFunction<ByteArray, Int, Event> { bytes, rssi ->
                Event(
                    Identifier.fromBytes(bytes),
                    rssi,
                    scope,
                    currentTimestampeProvider()
                )
            }
        )
    }

    private fun onConnectionError(e: Throwable) {
        bleEvents.scanFailureEvent()
        Timber.e("Connection failed with: $e")
    }

    private fun onReadError(e: Throwable) {
        bleEvents.disconnectDeviceEvent()
        Timber.e("Failed to read from remote device: $e")
    }

    private fun onReadSuccess(event: Event, macAddress: String) {
        Timber.d("Scanning Saving: $event")
        bleEvents.connectedDeviceEvent(event.identifier.asString, listOf(event.rssi))
        connectedDevices[macAddress] = event.identifier.asString

        saveContactWorker.createOrUpdateContactEvent(
            event.scope,
            event.identifier.asString,
            event.rssi,
            event.timestamp
        )
    }

    private data class Event(
        val identifier: Identifier,
        val rssi: Int,
        val scope: CoroutineScope,
        val timestamp: DateTime
    ) {
        override fun toString() = "Event[identifier: ${identifier.asString}, rssi: $rssi]"
    }
}
