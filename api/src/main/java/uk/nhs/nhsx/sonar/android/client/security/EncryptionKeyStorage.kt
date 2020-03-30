package uk.nhs.nhsx.sonar.android.client.security

interface EncryptionKeyStorage {
    fun provideKey(): ByteArray?
    fun putBase64Key(encodedKey: String)
}
