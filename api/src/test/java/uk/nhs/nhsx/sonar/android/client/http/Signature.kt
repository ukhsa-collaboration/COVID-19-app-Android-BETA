package uk.nhs.nhsx.sonar.android.client.http

import javax.crypto.KeyGenerator

fun generateSignatureKey(): ByteArray =
    KeyGenerator.getInstance("HMACSHA256").generateKey().getEncoded()
