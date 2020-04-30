/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import java.nio.ByteBuffer
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

class BluetoothIdSignerTest {
    private val keyStorage = mockk<SecretKeyStorage>()

    @Test
    fun `computes correct hmac`() {
        val signer = BluetoothIdSigner(keyStorage)
        every { keyStorage.provideSecretKey() } returns SecretKeySpec(
            Base64.getDecoder().decode("ddYTB7doP61hMnChfcWBVvvZsP5l5ypar5eeFQrBfY8="), "HMACSHA256"
        )
        val countryCode = "GB".toByteArray()
        val transmissionTime = (DateTime.parse("2020-04-24T14:00:00Z").millis / 1_000).toInt()
        val publicKeyBytes = Base64.getDecoder()
            .decode("Qz13WVPAVf4+AGWawrRWaoCbP5IzYbH0+p6v8uv0+gquL+zBNWwL5TG8wVRkoZSdITBrtr9W3rPGeJaR/nHFSg==")
        val randomBytes = Base64.getDecoder().decode("ZjSymR2t8ib8jrM6ovlw8bWCeU0PQzGC+Zo=")
        val tag = Base64.getDecoder().decode("NQvLtrm9KZljtJ+vD6NxaA==")

        val cryptogram = Cryptogram(
            publicKeyBytes,
            randomBytes,
            tag
        )
        val txPower = (-5).toByte()
        val timeBuffer = ByteBuffer.wrap(ByteArray(4)).apply {
            putInt(transmissionTime)
        }.array()

        val signature = signer.computeHmacSignature(
            countryCode,
            cryptogram.asBytes(),
            txPower,
            timeBuffer
        )

        assertThat(
            Base64.getEncoder().encodeToString(signature)
        ).isEqualTo("uJw5xR2ftV24vZ6UVroS8w==")
    }
}
