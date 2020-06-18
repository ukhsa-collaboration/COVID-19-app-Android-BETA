/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

fun generateSecretKey(): SecretKey {
    return KeyGenerator.getInstance("HMACSHA256").generateKey()
}

fun encodeBase64(byteArray: ByteArray): String =
    Base64.getEncoder().encodeToString(byteArray)

fun decodeBase64(string: String): ByteArray =
    Base64.getDecoder().decode(string)

fun generateSignature(
    secretKey: SecretKey,
    timestamp: String,
    body: ByteArray
): String {
    val mac = Mac.getInstance("HMACSHA256")
        .apply {
            init(secretKey)
            update(timestamp.toByteArray(Charsets.UTF_8))
        }

    val signature = mac.doFinal(body)

    return encodeBase64(signature)
}
