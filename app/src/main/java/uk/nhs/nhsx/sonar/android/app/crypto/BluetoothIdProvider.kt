/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothIdProvider @Inject constructor(
    private val cryptogramProvider: CryptogramProvider,
    private val bluetoothIdSigner: BluetoothIdSigner,
    private val currentDateProvider: () -> DateTime = { DateTime.now(DateTimeZone.UTC) }
) {
    // https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/bluetooth/le/BluetoothLeAdvertiser.java#144
    private val txPowerLevel = (-7).toByte()

    private val lock = Object()

    fun provideBluetoothPayload(): BluetoothIdentifier {
        synchronized(lock) {
            val cryptogram = cryptogramProvider.provideCryptogram()
            val transmissionTimeBytes = currentDateProvider().encodeAsSecondsSinceEpoch()
            val transmissionTime = ByteBuffer.wrap(transmissionTimeBytes).int
            val signature = bluetoothIdSigner.computeHmacSignature(COUNTRY_CODE, cryptogram.asBytes(), txPowerLevel, transmissionTimeBytes)
            return BluetoothIdentifier(
                COUNTRY_CODE,
                cryptogram,
                txPowerLevel,
                transmissionTime,
                signature
            )
        }
    }

    fun canProvideIdentifier(): Boolean = cryptogramProvider.canProvideCryptogram() && bluetoothIdSigner.canSign()
}
