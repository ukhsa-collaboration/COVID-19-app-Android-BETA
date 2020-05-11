/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.ByteBuffer

val COUNTRY_CODE: ByteArray = ByteBuffer.wrap(ByteArray(2)).putShort(826.toShort()).array()
const val PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME
const val ECDH = "ECDH"
const val ELLIPTIC_CURVE = "EC"
const val AES = "AES"
const val AES_GCM_NoPadding = "AES/GCM/NoPadding"
const val GCM_TAG_LENGTH = 128
const val EC_STANDARD_CURVE_NAME = "secp256r1"
