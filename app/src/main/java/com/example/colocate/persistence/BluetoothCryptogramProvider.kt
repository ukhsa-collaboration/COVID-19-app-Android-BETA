package com.example.colocate.persistence

import com.example.colocate.crypto.Cryptogram

interface BluetoothCryptogramProvider {
    fun provideBluetoothCryptogram(): Cryptogram
    fun canProvideCryptogram(): Boolean
}
