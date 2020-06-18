/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.joda.time.DateTime
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.SonarApplication
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import uk.nhs.nhsx.sonar.android.app.crypto.encodeAsSecondsSinceEpoch
import java.nio.ByteBuffer
import kotlin.random.Random

class TestProximityEvents(private val app: SonarApplication) {

    private var eventNumber = 0
    private val startTime = DateTime.parse("2020-04-01T14:33:13Z")
    private val currentTimestampProvider = {
        eventNumber++
        Timber.d("Sending event nr $eventNumber")
        when (eventNumber) {
            1 -> {
                startTime
            }
            2, 3 -> DateTime.parse("2020-04-01T14:34:43Z") // +90 seconds
            4 -> DateTime.parse("2020-04-01T14:44:53Z") // +610 seconds
            else -> throw IllegalStateException()
        }
    }
    val countryCode = "GB".toByteArray()
    val transmissionTime = ByteBuffer.wrap(startTime.encodeAsSecondsSinceEpoch()).int
    val firstDeviceSignature = Random.nextBytes(16)
    val secondDeviceSignature = Random.nextBytes(16)
    val firstDeviceId = BluetoothIdentifier(
        countryCode,
        Cryptogram.fromBytes(
            Random.nextBytes(Cryptogram.SIZE)
        ),
        -6,
        transmissionTime,
        firstDeviceSignature
    )
    val secondDeviceId =
        BluetoothIdentifier(
            countryCode,
            Cryptogram.fromBytes(
                Random.nextBytes(Cryptogram.SIZE)
            ),
            -8,
            transmissionTime + 90,
            secondDeviceSignature
        )

    private val testRxBleClient = TestRxBleClient(app)

    val testBluetoothModule =
        TestBluetoothModule(
            app,
            testRxBleClient,
            currentTimestampProvider,
            scanIntervalLength = 2
        )

    val testProximityEvent = TestProximityEvent(
        firstDeviceId = firstDeviceId,
        firstDeviceSignature = firstDeviceSignature,
        secondDeviceId = secondDeviceId,
        secondDeviceSignature = secondDeviceSignature,
        transmissionTime = transmissionTime,
        countryCode = countryCode
    )

    fun simulateUnsupportedDevice() {
        testBluetoothModule.simulateUnsupportedDevice = true
    }

    fun simulateTablet() {
        testBluetoothModule.simulateTablet = true
    }

    fun simulateDeviceInProximity() {
        val appComponent = app.appComponent as TestAppComponent
        val dao = appComponent.getAppDatabase().contactEventDao()

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                encryptedId = firstDeviceId.asBytes(),
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(10),
                txPower = -5
            ),
            ScanResultArgs(
                encryptedId = secondDeviceId.asBytes(),
                macAddress = "07-00-00-00-00-00",
                rssiList = listOf(40),
                txPower = -1
            )
        )

        await until {
            runBlocking { dao.getAll().size } == 2
        }

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                encryptedId = firstDeviceId.asBytes(),
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(20),
                txPower = -5
            )
        )

        await until {
            runBlocking { dao.get(firstDeviceId.cryptogram.asBytes())!!.rssiValues.size } == 2
        }

        testRxBleClient.emitScanResults(
            ScanResultArgs(
                encryptedId = firstDeviceId.asBytes(),
                macAddress = "06-00-00-00-00-00",
                rssiList = listOf(15),
                txPower = -5
            )
        )

        await until {
            runBlocking { dao.get(firstDeviceId.cryptogram.asBytes())!!.rssiValues.size } == 3
        }
    }
}

@Suppress("ArrayInDataClass")
data class TestProximityEvent(
    val firstDeviceId: BluetoothIdentifier,
    val firstDeviceSignature: ByteArray,
    val secondDeviceId: BluetoothIdentifier,
    val secondDeviceSignature: ByteArray,
    val transmissionTime: Int,
    val countryCode: ByteArray
)
