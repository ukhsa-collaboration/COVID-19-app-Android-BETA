package uk.nhs.nhsx.sonar.android.app.persistence

import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram

interface BluetoothCryptogramProvider {
    fun provideBluetoothCryptogram(): Cryptogram
    fun canProvideCryptogram(): Boolean
}
