package com.example.colocate.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named

@Module
class AppModule(private val applicationContext: Context) {
    @Provides
    fun provideContext() = applicationContext

    @Provides
    @Named(DISPATCHER_MAIN)
    fun mainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Named(DISPATCHER_IO)
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    companion object {
        const val DISPATCHER_MAIN = "DISPATCHER_MAIN"
        const val DISPATCHER_IO = "DISPATCHER_IO"
    }
}