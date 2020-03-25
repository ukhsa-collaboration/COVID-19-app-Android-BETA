package com.example.colocate.di

import com.example.colocate.ble.BluetoothService
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.BluetoothModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.PersistenceModule
import com.example.colocate.isolate.IsolateActivity
import dagger.Component

@Component(modules = [PersistenceModule::class, AppModule::class, BluetoothModule::class, NetworkModule::class])
interface ApplicationComponent {
    fun inject(bluetoothService: BluetoothService)
    fun inject(bluetoothService: IsolateActivity)
}




