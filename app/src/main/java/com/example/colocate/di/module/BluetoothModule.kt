/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import com.polidea.rxandroidble2.RxBleClient
import dagger.Module
import dagger.Provides

@Module
class BluetoothModule(private val applicationContext: Context) {
    @Provides
    fun provideBluetoothManager(): BluetoothManager =
        getSystemService(applicationContext, BluetoothManager::class.java)!!

    @Provides
    fun provideBluetoothScanner(bluetoothManager: BluetoothManager): BluetoothLeScanner =
        bluetoothManager.adapter.bluetoothLeScanner

    @Provides
    fun provideBluetoothAdvertiser(bluetoothManager: BluetoothManager): BluetoothLeAdvertiser =
        bluetoothManager.adapter.bluetoothLeAdvertiser

    @Provides
    fun provideRxBleClient(): RxBleClient =
        RxBleClient.create(applicationContext)
}
