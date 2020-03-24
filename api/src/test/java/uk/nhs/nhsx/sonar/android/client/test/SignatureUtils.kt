/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.test

import java.lang.RuntimeException
import java.security.NoSuchAlgorithmException
import javax.crypto.KeyGenerator

object SignatureUtils {
    fun generateKey() : ByteArray {
        try {
            return KeyGenerator.getInstance("HMACSHA256").generateKey().getEncoded()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }
}