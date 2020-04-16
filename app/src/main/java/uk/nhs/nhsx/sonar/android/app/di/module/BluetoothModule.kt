/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import com.polidea.rxandroidble2.RxBleClient
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.ble.BleEvents
import uk.nhs.nhsx.sonar.android.app.ble.LongLiveConnectionScan
import uk.nhs.nhsx.sonar.android.app.ble.SaveContactWorker
import uk.nhs.nhsx.sonar.android.app.ble.Scan
import uk.nhs.nhsx.sonar.android.app.ble.Scanner
import javax.inject.Named

@Module
open class BluetoothModule(
    private val applicationContext: Context,
    private val errorMarginInSeconds: Int,
    private val connectionV2: Boolean,
    private val encryptSonarId: Boolean
) {
    @Provides
    fun provideBluetoothManager(): BluetoothManager =
        getSystemService(applicationContext, BluetoothManager::class.java)!!

    @Provides
    fun provideBluetoothAdvertiser(bluetoothManager: BluetoothManager): BluetoothLeAdvertiser =
        bluetoothManager.adapter.bluetoothLeAdvertiser

    @Provides
    open fun provideRxBleClient(): RxBleClient =
        RxBleClient.create(applicationContext)

    @Provides
    open fun provideScanner(
        rxBleClient: RxBleClient,
        saveContactWorker: SaveContactWorker,
        bleEvents: BleEvents
    ): Scanner =
        if (connectionV2)
            LongLiveConnectionScan(rxBleClient, saveContactWorker, bleEvents = bleEvents)
        else
            Scan(rxBleClient, saveContactWorker, bleEvents, encryptSonarId = encryptSonarId)

    @Provides
    @Named(USE_CONNECTION_V2)
    fun provideUseConnectionV2() = connectionV2

    @Provides
    @Named(ENCRYPT_SONAR_ID)
    fun provideEncryptSonarId() = encryptSonarId

    @Provides
    @Named(ERROR_MARGIN)
    fun provideErrorMarginInSeconds() = errorMarginInSeconds

    companion object {
        const val USE_CONNECTION_V2 = "USE_CONNECTION_V2"
        const val ERROR_MARGIN = "ERROR_MARGIN"
        const val ENCRYPT_SONAR_ID = "ENCRYPT_SONAR_ID"
    }
}
