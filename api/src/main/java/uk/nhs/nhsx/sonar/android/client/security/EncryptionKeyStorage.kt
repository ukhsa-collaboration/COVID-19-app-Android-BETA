package uk.nhs.nhsx.sonar.android.client.security

interface EncryptionKeyStorage {
    fun provideKey(): ByteArray?
    fun putKey(key: ByteArray)
    fun putBase64Key(encodedKey: String)
}
