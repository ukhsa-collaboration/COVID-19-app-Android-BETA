/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di

import com.example.colocate.RegistrationNotificationService
import com.example.colocate.ble.BluetoothService
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.BluetoothModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.PersistenceModule
import com.example.colocate.di.module.RegistrationModule
import com.example.colocate.isolate.IsolateActivity
import com.example.colocate.registration.RegistrationUseCase
import dagger.Component
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        PersistenceModule::class,
        AppModule::class,
        BluetoothModule::class,
        NetworkModule::class,
        EncryptionKeyStorageModule::class,
        RegistrationModule::class
    ]
)
interface ApplicationComponent {
    fun inject(bluetoothService: BluetoothService)
    fun inject(bluetoothService: IsolateActivity)
    fun inject(registrationNotificationService: RegistrationNotificationService)

    fun registrationUseCase(): RegistrationUseCase
}
