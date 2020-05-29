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
import com.polidea.rxandroidble2.RxBleDevice
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

    private var knownDevices: MutableMap<String, BluetoothIdentifier> = mutableMapOf()
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

                connectToEachDiscoveredDevice(coroutineScope)

                devices.clear()
            }
        }
    }

    fun stop() {
        disposeScanDisposable()
        scanJob?.cancel()
    }

    private fun connectToEachDiscoveredDevice(coroutineScope: CoroutineScope) {
        devices.distinctBy { it.first.bleDevice }.forEach { (scanResult, txPowerAdvertised) ->
            val macAddress = scanResult.bleDevice.macAddress
            val device = scanResult.bleDevice
            val identifier = knownDevices[macAddress]
            if (identifier != null) {
                storeEvent(Event(identifier.asBytes(), scanResult.rssi, currentTimestampProvider()), coroutineScope, txPowerAdvertised)
            } else {
                Timber.d("Connecting to $macAddress")
                connectAndPerformOperation(
                    device,
                    macAddress,
                    txPowerAdvertised,
                    coroutineScope,
                    readIdAndRssi()
                )
            }
        }
    }

    private fun readIdAndRssi(): (RxBleConnection) -> Single<Event> =
        { connection ->
            negotiateMTU(connection)
                .flatMap {
                    disableRetry(it)
                }
                .flatMap {
                    Single.zip(
                        it.readCharacteristic(SONAR_IDENTITY_CHARACTERISTIC_UUID),
                        it.readRssi(),
                        BiFunction<ByteArray, Int, Event> { characteristicValue, rssi ->
                            Event(characteristicValue, rssi, currentTimestampProvider())
                        }
                    )
                }
        }

    private fun readOnlyRssi(identifier: BluetoothIdentifier): (RxBleConnection) -> Single<Event> =
        { connection: RxBleConnection ->
            connection.readRssi().flatMap { rssi ->
                Single.just(
                    Event(
                        identifier.asBytes(),
                        rssi,
                        currentTimestampProvider()
                    )
                )
            }
        }

    private fun connectAndPerformOperation(
        device: RxBleDevice,
        macAddress: String,
        txPowerAdvertised: Int,
        coroutineScope: CoroutineScope,
        readOperation: (RxBleConnection) -> Single<Event>
    ) {
        val compositeDisposable = CompositeDisposable()
        device
            .establishConnection(false)
            .flatMapSingle {
                readOperation(it)
            }
            .doOnSubscribe {
                compositeDisposable.add(it)
            }
            .take(1)
            .blockingSubscribe(
                { event ->
                    onReadSuccess(
                        event,
                        compositeDisposable,
                        macAddress,
                        txPowerAdvertised,
                        coroutineScope
                    )
                },
                { e -> onReadError(e, compositeDisposable, macAddress) }
            )
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
                ::scanError
            )

    private fun disableRetry(connection: RxBleConnection): Single<RxBleConnection> =
        connection.queue { bluetoothGatt, _, _ ->
            DisableRetryOnUnauthenticatedRead.bypassAuthenticationRetry(
                bluetoothGatt
            )
            Observable.just(connection)
        }.firstOrError()

    private fun onReadSuccess(
        event: Event,
        connectionDisposable: CompositeDisposable,
        macAddress: String,
        txPowerAdvertised: Int,
        scope: CoroutineScope
    ) {
        connectionDisposable.dispose()
        val identifier = BluetoothIdentifier.fromBytes(event.identifier)
        updateKnownDevices(identifier, macAddress)
        storeEvent(event, scope, txPowerAdvertised)
    }

    private fun onReadError(
        e: Throwable,
        connectionDisposable: CompositeDisposable,
        macAddress: String
    ) {
        connectionDisposable.dispose()
        Timber.e("failed reading from $macAddress - $e")
        eventEmitter.errorEvent(macAddress, e)
    }

    private fun updateKnownDevices(identifier: BluetoothIdentifier, macAddress: String) {
        val previousMac = knownDevices.entries.firstOrNull { (_, v) ->
            v.cryptogram.asBytes().contentEquals(identifier.cryptogram.asBytes())
        }
        knownDevices.remove(previousMac?.key)
        knownDevices[macAddress] = identifier
        Timber.d(
            "Previous MAC was ${previousMac?.key}, new is $macAddress for ${identifier.cryptogram.asBytes()
                .map { it.toInt() }
                .joinToString("")}")
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

    private fun scanError(e: Throwable) {
        Timber.e("Scan failed with: $e")
    }

    private fun storeEvent(event: Event, scope: CoroutineScope, txPowerAdvertised: Int) {
        Timber.d("Event $event")
        eventEmitter.successfulContactEvent(
            event.identifier,
            listOf(event.rssi),
            txPowerAdvertised
        )

        saveContactWorker.createOrUpdateContactEvent(
            scope,
            event.identifier,
            event.rssi,
            event.timestamp,
            txPowerAdvertised
        )
    }

    // We're really just using this as a bundle, and never comparing different events.
    @Suppress("ArrayInDataClass")
    private data class Event(
        val identifier: ByteArray,
        val rssi: Int,
        val timestamp: DateTime
    )
}
