/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import com.example.colocate.ble.LongLiveConnectionScan
import com.example.colocate.ble.SaveContactWorker
import com.example.colocate.ble.Scan
import com.example.colocate.ble.Scanner
import com.polidea.rxandroidble2.RxBleClient
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class BluetoothModule(
    private val applicationContext: Context,
    private val connectionV2: Boolean
) {
    @Provides
    fun provideBluetoothManager(): BluetoothManager =
        getSystemService(applicationContext, BluetoothManager::class.java)!!

    @Provides
    fun provideBluetoothAdvertiser(bluetoothManager: BluetoothManager): BluetoothLeAdvertiser =
        bluetoothManager.adapter.bluetoothLeAdvertiser

    @Provides
    fun provideRxBleClient(): RxBleClient =
        RxBleClient.create(applicationContext)

    @Provides
    fun provideScanner(rxBleClient: RxBleClient, saveContactWorker: SaveContactWorker): Scanner =
        if (connectionV2) LongLiveConnectionScan(rxBleClient, saveContactWorker)
        else Scan(rxBleClient, saveContactWorker)

    @Provides
    @Named(USE_CONNECTION_V2)
    fun provideUseConnectionV2() = connectionV2

    companion object {
        const val USE_CONNECTION_V2 = "USE_CONNECTION_V2"
    }
}
