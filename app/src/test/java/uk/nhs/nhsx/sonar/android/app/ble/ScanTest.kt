/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.lachlanmckee.timberjunit.TimberTestRule
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import java.util.Base64
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class ScanTest {

    private val bleClient = mockk<RxBleClient>()
    private val scanResult = mockk<ScanResult>()
    private val bleDevice = mockk<RxBleDevice>()
    private val connection = mockk<RxBleConnection>()
    private val saveContactWorker = mockk<SaveContactWorker>()

    private val timestamp = DateTime.now(DateTimeZone.UTC)
    private val rssi = -50
    private val period = 50L
    private lateinit var identifier: ByteArray

    @Rule
    @JvmField
    val logAllOnFailuresRule: TimberTestRule = TimberTestRule.logAllWhenTestFails()

    @Before
    fun setUp() {
        Timber.plant(Timber.DebugTree())
        every { bleClient.observeStateChanges() } returns Observable.empty()
        every { bleClient.state } returns RxBleClient.State.READY
        every {
            bleClient.scanBleDevices(
                any<ScanSettings>(),
                any(),
                any()
            )
        } returns Observable.just(scanResult)

        every { scanResult.bleDevice } returns bleDevice
        every { bleDevice.connectionState } returns RxBleConnection.RxBleConnectionState.DISCONNECTED
        every { bleDevice.macAddress } returns "00:1B:44:11:3A:B7"

        every { bleDevice.establishConnection(false) } returns Observable.merge(
            Observable.just(connection),
            Observable
                .timer(period + 25, TimeUnit.MILLISECONDS)
                .flatMap {
                    val disconnectException = BleDisconnectedException.adapterDisabled("")
                    Observable.error<RxBleConnection>(disconnectException)
                }
        )
        every { connection.requestMtu(BluetoothIdentifier.SIZE + 2) } returns Single.just(BluetoothIdentifier.SIZE + 2)
        every { connection.readRssi() } returns Single.just(rssi)

        identifier = ByteArray(BluetoothIdentifier.SIZE) { 1 }
        every { connection.readCharacteristic(DEVICE_CHARACTERISTIC_UUID) } returns Single.just(
            identifier
        )
    }

    @Test
    fun connectionWithSingularDevice() = runBlocking {
        val coroutineScope = this
        val scan = Scan(
            bleClient,
            saveContactWorker,
            currentTimestampProvider = { timestamp },
            bleEvents = BleEvents { Base64.getEncoder().encodeToString(it) },
            scanIntervalLength = 1
        )

        scan.start(coroutineScope)

        try {
            withContext(Dispatchers.Default) {
                verify(timeout = 3_000) {
                    saveContactWorker.createOrUpdateContactEvent(coroutineScope, identifier, rssi, timestamp)
                }
            }
        } finally {
            scan.stop()
        }
    }
}
