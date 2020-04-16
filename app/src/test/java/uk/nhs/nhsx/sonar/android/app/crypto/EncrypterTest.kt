/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.KDF2BytesGenerator
import org.bouncycastle.crypto.params.KDFParameters
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.KeyStorage
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.spec.ECPublicKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

const val exampleServerPubPEM = """-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEu1f68MqDXbKeTqZMTHsOGToO4rKn
PClXe/kE+oWqlaWZQv4J1E98cUNdpzF9JIFRPMCNdGOvTr4UB+BhQv9GWg==
-----END PUBLIC KEY-----"""

const val exampleLocalPubPEM = """-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAENzxiX7nXKNKhP2gVS01X04sCN8e4FoE+fXomHEDRq/GfUFQDOrQL7O7JDe6m09nzRXLxIwYb3Gr412Q5JvIe5A==
-----END PUBLIC KEY-----"""

const val exampleLocalPrivPEM = """-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg9+/o1cA5BwfbHgZFMph5i9C8MPdjUYxgbCvFO3aeh8ShRANCAAQ3PGJfudco0qE/aBVLTVfTiwI3x7gWgT59eiYcQNGr8Z9QVAM6tAvs7skN7qbT2fNFcvEjBhvcavjXZDkm8h7k
-----END PRIVATE KEY-----"""

const val exampleServerPrivPEM = """-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgLCloV6m2p78+7EYHJyyniGdpLxiPhWk+q2AozMlWT4ahRANCAAS7V/rwyoNdsp5OpkxMew4ZOg7isqc8KVd7+QT6haqVpZlC/gnUT3xxQ12nMX0kgVE8wI10Y69OvhQH4GFC/0Za
-----END PRIVATE KEY-----"""

const val knownPayload = "4608173DB4E454C52B9D5D900152509C"
const val knownTag = "8F2DEB80A07AC5B89BA7BD3D6BF3B5B8"
const val knownSharedSecret = "1EFF9D676FDBE0AEDBE914DB7F8F72E485870DC2554BE5B2D2DB52C6BEEE5F7A"
const val knownDerivedKey = "F35CEB3A03CA0D6707D3DA754698F326"
const val knownInitialisationVector = "470D6E206CB55D2890558C01B315A7E0"

private val knownXCoordinate = "373C625FB9D728D2A13F68154B4D57D38B0237C7B816813E7D7A261C40D1ABF1"
private val knownYCoordinate = "9F5054033AB40BECEEC90DEEA6D3D9F34572F123061BDC6AF8D7643926F21EE4"

class EncrypterTest {

    @Before
    fun setUp() {
        val bouncyCastleProvider = org.bouncycastle.jce.provider.BouncyCastleProvider()
        Security.insertProviderAt(bouncyCastleProvider, 1)
    }

    @Test
    fun `produces the expected encrypted payload`() {
        val ephemeralKeyProvider = mockk<EphemeralKeyProvider>()
        val keyStorage = mockk<KeyStorage>()
        every { keyStorage.providePublicKey() } returns loadPublicKey(exampleServerPubPEM)
        every { ephemeralKeyProvider.providePublicKey() } returns loadPublicKey(exampleLocalPubPEM) as BCECPublicKey
        val x = knownXCoordinate.hexStringToByteArray()
        val y = knownYCoordinate.hexStringToByteArray()
        every { ephemeralKeyProvider.providePublicKeyPoint() } returns byteArrayOf(0x04) + x + y
        every { ephemeralKeyProvider.providePrivateKey() } returns loadPrivateKey(exampleLocalPrivPEM)

        val encrypter = Encrypter(keyStorage, ephemeralKeyProvider)

        val plainText = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee").uuidBytes()

        val cryptogram = encrypter.encrypt(plainText)

        // strips away the leading 0x04, to save a byte
        assertThat(cryptogram.publicKeyBytes).hasSize(64)
        assertThat(cryptogram.publicKeyBytes).isEqualTo(knownXCoordinate.hexStringToByteArray() + knownYCoordinate.hexStringToByteArray())

        assertThat(cryptogram.encryptedPayload).hasSize(16)
        assertThat(cryptogram.encryptedPayload.toHexString()).isEqualTo(knownPayload)

        assertThat(cryptogram.tag).hasSize(16)
        assertThat(cryptogram.tag.toHexString()).isEqualTo(knownTag)
    }

    @Test
    fun `can be decrypted from server private key and the local public key bytes`() {
        val x =
            "373C625FB9D728D2A13F68154B4D57D38B0237C7B816813E7D7A261C40D1ABF1".hexStringToByteArray()
        val y =
            "9F5054033AB40BECEEC90DEEA6D3D9F34572F123061BDC6AF8D7643926F21EE4".hexStringToByteArray()
        val knownCryptogram =
            x + y + knownPayload.hexStringToByteArray() + knownTag.hexStringToByteArray()

        val serverPrivateKey = loadPrivateKey(exampleServerPrivPEM)
        val localPublicKey = localPublicKeyFromCryptogram(knownCryptogram)
        assertThat(localPublicKey)
            .isEqualTo(loadPublicKey(exampleLocalPubPEM))

        val sharedSecret = generateSharedSecret(serverPrivateKey, localPublicKey)
        assertThat(sharedSecret.encoded).hasSize(32)
        assertThat(sharedSecret.encoded.toHexString())
            .isEqualTo(knownSharedSecret)

        val (keyBytes, iv) = deriveKey(sharedSecret, x, y)
        assertThat(keyBytes.toHexString())
            .isEqualTo(knownDerivedKey)
        assertThat(iv.toHexString())
            .isEqualTo(knownInitialisationVector)
        val key = SecretKeySpec(keyBytes, AES)

        val decrypted = decrypt(
            payload = knownPayload.hexStringToByteArray(),
            tag = knownTag.hexStringToByteArray(),
            key = key,
            iv = iv
        )

        assertThat(decrypted).isEqualTo(
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee").uuidBytes()
        )
    }

    fun decrypt(
        payload: ByteArray,
        tag: ByteArray,
        key: SecretKey,
        iv: ByteArray
    ): ByteArray {
        val c: Cipher = Cipher.getInstance(AES_GCM_NoPadding, PROVIDER_NAME)
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return c.doFinal(payload + tag)
    }

    fun deriveKey(sharedSecret: SecretKey, x: ByteArray, y: ByteArray): Pair<ByteArray, ByteArray> {
        val kdfGenerator = KDF2BytesGenerator(SHA256Digest())
        kdfGenerator.init(KDFParameters(sharedSecret.encoded, byteArrayOf(0x04) + x + y))
        val kdfOutput = ByteArray(32)
        kdfGenerator.generateBytes(kdfOutput, 0, 32)

        return Pair(
            kdfOutput.sliceArray((0 until 16)),
            kdfOutput.sliceArray((16 until 32))
        )
    }

    fun generateSharedSecret(
        serverPrivateKey: PrivateKey,
        localPublicKey: PublicKey
    ): SecretKeySpec {
        val keyAgreement = KeyAgreement.getInstance(ECDH, PROVIDER_NAME)
        keyAgreement.init(serverPrivateKey)
        keyAgreement.doPhase(localPublicKey, true)
        return SecretKeySpec(keyAgreement.generateSecret(), AES)
    }

    fun localPublicKeyFromCryptogram(cryptogram: ByteArray): PublicKey {
        // leading 0x04 to specify that the points are compressed
        val encodedKeyPoints = byteArrayOf(0x04) + cryptogram.sliceArray((0 until 64))
        val spec = ECNamedCurveTable.getParameterSpec(EC_STANDARD_CURVE_NAME)
        val params = ECNamedCurveSpec(EC_STANDARD_CURVE_NAME, spec.curve, spec.g, spec.n)
        val publicPoint = ECPointUtil.decodePoint(params.curve, encodedKeyPoints)
        val pubKeySpec = ECPublicKeySpec(publicPoint, params)
        return KeyFactory.getInstance("EC", PROVIDER_NAME).generatePublic(pubKeySpec)
    }

    fun ByteArray.toHexString(): String =
        this.joinToString("") { String.format("%02X", it) }

    fun String.hexStringToByteArray() =
        ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    fun loadPublicKey(publicPEM: String): PublicKey {
        val ecKeyFactory = KeyFactory.getInstance(ELLIPTIC_CURVE, PROVIDER_NAME)
        val pubKeyDER: ByteArray = PEMtoDER(publicPEM)
        val pubKeySpec = X509EncodedKeySpec(pubKeyDER)
        return ecKeyFactory.generatePublic(pubKeySpec)
    }

    fun loadPrivateKey(privatePEM: String): PrivateKey {
        val ecKeyFactory = KeyFactory.getInstance(ELLIPTIC_CURVE, PROVIDER_NAME)
        val privKeyDER: ByteArray = PEMtoDER(privatePEM)
        val localPrivKeySpec = PKCS8EncodedKeySpec(privKeyDER)
        return ecKeyFactory.generatePrivate(localPrivKeySpec)
    }

    fun PEMtoDER(pemString: String): ByteArray {
        // Strip header and footer
        var base64PEM: String = pemString
        // Remove any public key headers and footers
        base64PEM = base64PEM.replace("-----BEGIN PUBLIC KEY-----\n", "")
        base64PEM = base64PEM.replace("-----END PUBLIC KEY-----", "")
        // Remove any private key headers and footers
        base64PEM = base64PEM.replace("-----BEGIN PRIVATE KEY-----\n", "")
        base64PEM = base64PEM.replace("-----END PRIVATE KEY-----", "")

        // Remote any whitespace
        base64PEM = base64PEM.replace("\\s".toRegex(), "")

        // Decode base64 to get DER format and return

        return Base64.getDecoder().decode(base64PEM)
    }

    private fun UUID.uuidBytes(): ByteArray {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(this.mostSignificantBits)
        bb.putLong(this.leastSignificantBits)
        return bb.array()
    }
}
