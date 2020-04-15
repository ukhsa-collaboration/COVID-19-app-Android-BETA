/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.bluetooth.BluetoothAdapter
import android.os.ParcelUuid
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleAlreadyConnectedException
import com.polidea.rxandroidble2.exceptions.BleException
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.CoroutineScope
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.Seconds
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LongLiveConnectionScan @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val saveContactWorker: SaveContactWorker,
    private val startTimestampProvider: () -> DateTime = { DateTime.now(UTC) },
    private val endTimestampProvider: () -> DateTime = startTimestampProvider,
    private val periodInMilliseconds: Long = 20_000,
    private val bleEvents: BleEvents
) : Scanner {
    private val coLocateServiceUuidFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
        .build()

    private var compositeDisposable = CompositeDisposable()

    private var bluetoothWasDisabledToHandleApplicationRegistrationFailure = false

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

    private val macAddressToRecord = mutableMapOf<String, SaveContactWorker.Record>()

    override fun start(coroutineScope: CoroutineScope) {
        Timber.d("LongLiveConnectionScan start")

        val flowDisposable = rxBleClient
            .observeStateChanges()
            .startWith(rxBleClient.state)
            .switchMap { state ->
                Timber.d("LongLiveConnectionScan state = $state")

                when (state) {
                    RxBleClient.State.BLUETOOTH_NOT_AVAILABLE -> {
                        if (bluetoothWasDisabledToHandleApplicationRegistrationFailure) {
                            BluetoothAdapter.getDefaultAdapter().enable()
                            bluetoothWasDisabledToHandleApplicationRegistrationFailure = false
                        }
                        Observable.empty()
                    }
                    RxBleClient.State.READY ->
                        rxBleClient.scanBleDevices(
                            settings,
                            coLocateBackgroundedIPhoneFilter,
                            coLocateServiceUuidFilter
                        ).onErrorResumeNext { throwable: Throwable ->
                            Timber.e(throwable, "LongLiveConnectionScan scan failure")
                            if (throwable is BleScanException &&
                                throwable.reason == BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED
                            ) {
                                BluetoothAdapter.getDefaultAdapter().disable()
                                bluetoothWasDisabledToHandleApplicationRegistrationFailure = true
                            }
                            Observable.empty()
                        }
                    else -> Observable.empty<ScanResult>()
                }
            }
            .filter {
                Timber.d("Scan result ${it.bleDevice.connectionState}")
                it.bleDevice.connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED
            }
            .subscribe(
                { result -> connectAndCaptureEvents(result.bleDevice, coroutineScope) },
                ::onScanError
            )
        compositeDisposable.add(flowDisposable)
    }

    private fun connectAndCaptureEvents(
        device: RxBleDevice,
        coroutineScope: CoroutineScope
    ) {
        val macAddress = device.macAddress
        val connectionDisposable = device
            .establishConnection(false)
            .flatMap { connection -> captureContactEvents(connection, macAddress) }
            .subscribe(::onReadSuccess) { exception ->
                onDisconnect(
                    exception,
                    macAddress,
                    coroutineScope
                )
            }

        compositeDisposable.add(connectionDisposable)
    }

    private fun captureContactEvents(
        connection: RxBleConnection,
        macAddress: String
    ): Observable<Event> {
        return Observable.combineLatest(
            readIdentifierAndCreateRecord(connection, macAddress),
            readRssiPeriodically(connection),
            createEvent(macAddress)
        )
    }

    private fun onDisconnect(
        exception: Throwable?,
        macAddress: String,
        coroutineScope: CoroutineScope
    ) {
        when (exception) {
            is BleAlreadyConnectedException -> Timber.d(exception, "Already connected $macAddress")
            is BleException -> saveRecord(macAddress, coroutineScope)
            else -> Timber.d(exception, "Failed to connect to the device $macAddress")
        }
    }

    private fun saveRecord(
        macAddress: String,
        coroutineScope: CoroutineScope
    ) {
        val record = macAddressToRecord.remove(macAddress)

        if (record != null) {
            val duration = Seconds.secondsBetween(startTimestampProvider(), endTimestampProvider())
            val finalRecord = record.copy(duration = duration.seconds)
            Timber.d("Save record: $finalRecord")
            saveContactWorker.saveContactEvent(
                coroutineScope,
                finalRecord
            )
            bleEvents.disconnectDeviceEvent(record.sonarId.asString)
        } else {
            Timber.e("Disconnecting from $macAddress without having read sonarID")
        }
    }

    private fun createEvent(macAddress: String): BiFunction<Identifier, Int, Event> {
        return BiFunction<Identifier, Int, Event> { identifier, rssi ->
            Event(
                macAddress,
                identifier,
                rssi
            )
        }
    }

    private fun readRssiPeriodically(connection: RxBleConnection) =
        Observable.interval(0, periodInMilliseconds, TimeUnit.MILLISECONDS)
            .flatMapSingle { connection.readRssi() }

    private fun readIdentifierAndCreateRecord(connection: RxBleConnection, macAddress: String) =
        connection
            .readCharacteristic(DEVICE_CHARACTERISTIC_UUID)
            .map { bytes -> Identifier.fromBytes(bytes) }.toObservable()
            .doOnNext { identifier ->
                macAddressToRecord[macAddress] =
                    SaveContactWorker.Record(
                        timestamp = startTimestampProvider(),
                        sonarId = identifier
                    )
            }

    override fun stop() {
        Timber.d("LongLiveConnectionScan stop")
        compositeDisposable.clear()
    }

    private fun onScanError(e: Throwable) {
        Timber.e("Scan failed with: $e")
        bleEvents.scanFailureEvent()
    }

    private fun onReadSuccess(event: Event) {
        macAddressToRecord[event.macAddress]?.let { record ->
            record.rssiValues.add(event.rssi)
            bleEvents.connectedDeviceEvent(record.sonarId.asString, record.rssiValues)
        }
    }

    private data class Event(
        val macAddress: String,
        val identifier: Identifier,
        val rssi: Int
    ) {
        override fun toString() = "Event[identifier: ${identifier.asString}, rssi: $rssi]"
    }
}
