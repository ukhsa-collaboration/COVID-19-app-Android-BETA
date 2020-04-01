/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import android.os.ParcelUuid
import com.example.colocate.di.module.AppModule
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ContactEventV2Dao
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class LongLiveConnectionScan @Inject constructor(
    private val rxBleClient: RxBleClient,
    private val contactEventDao: ContactEventDao,
    private val contactEventV2Dao: ContactEventV2Dao,
    @Named(AppModule.DISPATCHER_IO) private val dispatcher: CoroutineDispatcher
) : Scanner {
    private val coLocateServiceUuidFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
        .build()

    private var compositeDisposable = CompositeDisposable()

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
        val scanDisposable = rxBleClient.scanBleDevices(
            settings,
            coLocateBackgroundedIPhoneFilter,
            coLocateServiceUuidFilter
        )
            .filter { it.bleDevice.connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED }
            .subscribe(
                { result ->
                    val connectionDisposable = result.bleDevice.establishConnection(false)
                        .flatMap { connection ->
                            Observable.combineLatest(
                                readIdentifier(connection).doOnNext { identifier ->
                                    macAddressToRecord[result.bleDevice.macAddress] =
                                        SaveContactWorker.Record(
                                            timestamp = Date(),
                                            sonarId = identifier
                                        )
                                },
                                readRssiPeriodically(connection),
                                createEvent(result.bleDevice.macAddress, coroutineScope)
                            )
                        }
                        .subscribe(
                            ::onReadSuccess,
                            { e ->
                                Timber.e(
                                    e,
                                    "Failed to read from remote device with mac-address: ${result.bleDevice.macAddress}"
                                )
                                val record = macAddressToRecord.remove(result.bleDevice.macAddress)
                                if (record != null) {
                                    val duration = (Date().time - record.timestamp.time) / 1000
                                    val finalRecord = record.copy(duration = duration)
                                    Timber.d("Save record: $finalRecord")
                                    SaveContactWorker(
                                        dispatcher,
                                        contactEventDao,
                                        contactEventV2Dao
                                    ).saveContactEventV2(coroutineScope, finalRecord)
                                }
                            },
                            ::onDisconnect
                        )
                    compositeDisposable.add(connectionDisposable)
                },
                ::onScanError
            )
        compositeDisposable.add(scanDisposable)
    }

    private fun onDisconnect() {
        Timber.d("Scanning Disconnected")
    }

    private fun createEvent(
        macAddress: String,
        coroutineScope: CoroutineScope
    ): BiFunction<Identifier, Int, Event> {
        return BiFunction<Identifier, Int, Event> { identifier, rssi ->
            Event(
                macAddress,
                identifier,
                rssi,
                coroutineScope
            )
        }
    }

    private fun readRssiPeriodically(connection: RxBleConnection) =
        Observable.interval(0, 10, TimeUnit.SECONDS).flatMapSingle { connection.readRssi() }

    private fun readIdentifier(connection: RxBleConnection) =
        connection.readCharacteristic(DEVICE_CHARACTERISTIC_UUID)
            .map { bytes -> Identifier.fromBytes(bytes) }.toObservable()

    override fun stop() {
        compositeDisposable.clear()
    }

    private fun onScanError(e: Throwable) = Timber.e("Scan failed with: $e")

    private fun onReadSuccess(event: Event) {
        Timber.d("Scanning Saving: $event")
        val record = macAddressToRecord[event.macAddress]
        record?.rssiValues?.add(event.rssi)
    }

    private data class Event(
        val macAddress: String,
        val identifier: Identifier,
        val rssi: Int,
        val scope: CoroutineScope
    ) {
        override fun toString() = "Event[identifier: ${identifier.asString}, rssi: $rssi]"
    }
}
