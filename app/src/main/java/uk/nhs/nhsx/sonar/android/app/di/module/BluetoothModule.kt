/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import com.polidea.rxandroidble2.RxBleClient
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.BuildConfig
import uk.nhs.nhsx.sonar.android.app.DeviceDetection
import uk.nhs.nhsx.sonar.android.app.ble.DebugBleEventTracker
import uk.nhs.nhsx.sonar.android.app.ble.NoOpBleEventEmitter
import uk.nhs.nhsx.sonar.android.app.ble.SaveContactWorker
import uk.nhs.nhsx.sonar.android.app.ble.Scanner
import javax.inject.Named

@Module
open class BluetoothModule(
    private val applicationContext: Context,
    private val scanIntervalLength: Int
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
    open fun provideDeviceDetection(): DeviceDetection =
        DeviceDetection(BluetoothAdapter.getDefaultAdapter(), applicationContext)

    @Provides
    open fun provideScanner(
        rxBleClient: RxBleClient,
        saveContactWorker: SaveContactWorker,
        debugBleEventEmitter: DebugBleEventTracker,
        noOpBleEventEmitter: NoOpBleEventEmitter
    ): Scanner {
        val eventEmitter = when (BuildConfig.BUILD_TYPE) {
            "debug", "staging" -> debugBleEventEmitter
            else -> noOpBleEventEmitter
        }
        return Scanner(
            rxBleClient,
            saveContactWorker,
            eventEmitter,
            scanIntervalLength = scanIntervalLength
        )
    }

    @Provides
    @Named(SCAN_INTERVAL_LENGTH)
    fun provideScanIntervalLength() = scanIntervalLength

    companion object {
        const val SCAN_INTERVAL_LENGTH = "SCAN_INTERVAL_LENGTH"
    }
}
