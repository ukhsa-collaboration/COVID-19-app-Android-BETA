/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattService
import android.content.Context
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.polidea.rxandroidble2.RxBleScanResult
import com.polidea.rxandroidble2.internal.scan.ScanRecordImplCompat
import com.polidea.rxandroidble2.scan.BackgroundScanner
import com.polidea.rxandroidble2.scan.ScanCallbackType.CALLBACK_TYPE_ALL_MATCHES
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Emitter
import io.reactivex.Observable
import org.assertj.core.api.Assertions.fail
import org.joda.time.Instant
import uk.nhs.nhsx.sonar.android.app.ble.DEVICE_CHARACTERISTIC_UUID
import uk.nhs.nhsx.sonar.android.app.ble.Identifier
import java.util.UUID

class TestRxBleClient(context: Context) : RxBleClient() {

    private val realClient = create(context)
    private var emitter: Emitter<ScanResult>? = null

    override fun getBleDevice(macAddress: String): RxBleDevice = realClient.getBleDevice(macAddress)
    override fun isScanRuntimePermissionGranted() = realClient.isScanRuntimePermissionGranted
    override fun getRecommendedScanRuntimePermissions(): Array<String> =
        realClient.recommendedScanRuntimePermissions

    override fun getBackgroundScanner(): BackgroundScanner = realClient.backgroundScanner
    override fun getBondedDevices(): MutableSet<RxBleDevice> = realClient.bondedDevices

    override fun scanBleDevices(vararg filterServiceUUIDs: UUID): Observable<RxBleScanResult> =
        fail("Not available")

    override fun getState(): State =
        stateIgnoringLocationServicesNotEnabled(realClient.state)

    override fun observeStateChanges(): Observable<State> =
        realClient
            .observeStateChanges()
            .map(::stateIgnoringLocationServicesNotEnabled)

    private fun stateIgnoringLocationServicesNotEnabled(state: State): State =
        when (state) {
            State.LOCATION_SERVICES_NOT_ENABLED -> State.READY
            else -> state
        }

    override fun scanBleDevices(scanSettings: ScanSettings, vararg scanFilters: ScanFilter): Observable<ScanResult> =
        Observable.create { e -> emitter = e }

    fun emitScanResults(vararg args: ScanResultArgs) {
        args.map {
            val scanResult = createScanResult(it)
            emitter!!.onNext(scanResult)
        }
    }

    private fun createScanResult(args: ScanResultArgs): ScanResult {
        val scanRecord =
            ScanRecordImplCompat(null, null, null, -1, Int.MIN_VALUE, null, ByteArray(0))
        val characteristicUuid = DEVICE_CHARACTERISTIC_UUID

        val characteristic =
            BluetoothGattCharacteristic(characteristicUuid, PROPERTY_READ, PERMISSION_READ)
        characteristic.value = Identifier.fromString(args.sonarId.toString()).asBytes

        val service = BluetoothGattService(characteristicUuid, 0)
        service.addCharacteristic(characteristic)

        val services = RxBleDeviceServices(mutableListOf<BluetoothGattService>())
        services.bluetoothGattServices.add(service)

        val rxBleDevice = TestBluetoothDevice(args.macAddress, services, args.rssiList)
        val timestamp = Instant.now().millis * 1000

        return ScanResult(rxBleDevice, -1, timestamp, CALLBACK_TYPE_ALL_MATCHES, scanRecord)
    }
}

data class ScanResultArgs(
    val sonarId: UUID,
    val macAddress: String,
    val rssiList: List<Int>
)
