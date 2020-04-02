package com.example.colocate

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.content.Context
import com.example.colocate.ble.DEVICE_CHARACTERISTIC_UUID
import com.example.colocate.ble.Identifier
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.RxBleScanResult
import com.polidea.rxandroidble2.internal.scan.ScanRecordImplCompat
import com.polidea.rxandroidble2.mockrxandroidble.RxBleClientMock
import com.polidea.rxandroidble2.scan.BackgroundScanner
import com.polidea.rxandroidble2.scan.ScanCallbackType.CALLBACK_TYPE_ALL_MATCHES
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Emitter
import io.reactivex.Observable
import org.assertj.core.api.Assertions.fail
import java.time.Instant
import java.util.UUID

class TestRxBleClient(context: Context) : RxBleClient() {

    private val realClient = create(context)
    private var emitter: Emitter<ScanResult>? = null

    override fun getBleDevice(macAddress: String): RxBleDevice = realClient.getBleDevice(macAddress)
    override fun getState(): State = realClient.state
    override fun isScanRuntimePermissionGranted() = realClient.isScanRuntimePermissionGranted
    override fun getRecommendedScanRuntimePermissions(): Array<String> = realClient.recommendedScanRuntimePermissions
    override fun getBackgroundScanner(): BackgroundScanner = realClient.backgroundScanner
    override fun getBondedDevices(): MutableSet<RxBleDevice> = realClient.bondedDevices

    override fun scanBleDevices(vararg filterServiceUUIDs: UUID): Observable<RxBleScanResult> = fail("Not available")
    override fun observeStateChanges(): Observable<State> = fail("Not available")

    override fun scanBleDevices(scanSettings: ScanSettings, vararg scanFilters: ScanFilter): Observable<ScanResult> =
        Observable.create { e -> emitter = e }

    fun emitScanResults(vararg devices: TestBluetoothDevice) {
        devices.map {
            val scanResult = createScanResult(it)
            emitter!!.onNext(scanResult)
        }
    }

    private fun createScanResult(device: TestBluetoothDevice): ScanResult {
        val scanRecord = ScanRecordImplCompat(null, null, null, -1, Int.MIN_VALUE, null, ByteArray(0))
        val characteristicUuid = DEVICE_CHARACTERISTIC_UUID
        val characteristic = BluetoothGattCharacteristic(characteristicUuid, PROPERTY_READ, PERMISSION_READ)
        characteristic.value = Identifier.fromString(device.eventUUID.toString()).asBytes

        val rxBleDevice = RxBleClientMock.DeviceBuilder()
            .deviceMacAddress(device.macAddress)
            .rssi(device.rssi)
            .scanRecord(ByteArray(0))
            .addService(characteristicUuid, listOf(characteristic))
            .build()

        val timestamp = Instant.now().toEpochMilli() * 1000

        return ScanResult(rxBleDevice, device.rssi, timestamp, CALLBACK_TYPE_ALL_MATCHES, scanRecord)
    }
}

data class TestBluetoothDevice(val eventUUID: UUID, val macAddress: String, val rssi: Int)
