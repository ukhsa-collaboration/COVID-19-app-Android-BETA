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
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.lachlanmckee.timberjunit.TimberTestRule
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ScanTest {

    private val bleClient = mockk<RxBleClient>()
    private val scanResult = mockk<ScanResult>()
    private val bleDevice = mockk<RxBleDevice>()
    private val connection = mockk<RxBleConnection>()
    private val saveContactWorker = FakeSaveContactWorker()

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
        every { connection.requestMtu(108) } returns Single.just(108)
        every { connection.readRssi() } returns Single.just(rssi)

        identifier = ByteArray(64) { 1 }
        every { connection.readCharacteristic(DEVICE_CHARACTERISTIC_UUID) } returns Single.just(
            ByteArray(64) { 1 }
        )
    }

    // TODO: Speed up - probably by making scan extension configurable
    @Test
    fun connectionWithSingularDevice() {
        runBlocking {
            val sut = Scan(
                bleClient,
                saveContactWorker,
                currentTimestampProvider = { timestamp },
                bleEvents = BleEvents(),
                encryptSonarId = false,
                scanIntervalLength = 1
            )
            sut.start(this)

            withContext(Dispatchers.Default) {
                await untilNotNull { saveContactWorker.savedId }
            }

            assertThat(saveContactWorker.savedId).isEqualTo(identifier)
            assertThat(saveContactWorker.savedRssi).isEqualTo(rssi)
            assertThat(saveContactWorker.savedTimestamp).isEqualTo(timestamp)

            assertThat(saveContactWorker.saveScope).isEqualTo(this)
            sut.stop()
        }
    }
}

private class FakeSaveContactWorker : SaveContactWorker by mockk() {
    var saveScope: CoroutineScope? = null
        private set
    var savedId: ByteArray? = null
        private set
    var savedRssi: Int? = null
        private set
    var savedTimestamp: DateTime? = null
        private set

    override fun createOrUpdateContactEvent(
        scope: CoroutineScope,
        id: ByteArray,
        rssi: Int,
        timestamp: DateTime
    ) {
        saveScope = scope
        savedId = id
        savedRssi = rssi
        savedTimestamp = timestamp
    }
}
