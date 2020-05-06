/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper
import javax.inject.Named

@Module
class AppModule(
    private val applicationContext: Context,
    private val locationHelper: LocationHelper,
    private val sonarAnalytics: SonarAnalytics
) {

    @Provides
    fun provideContext() = applicationContext

    @Provides
    fun provideLocationHelper() = locationHelper

    @Provides
    @Named(DISPATCHER_DEFAULT)
    fun defaultDispatcher(): CoroutineDispatcher =
        Dispatchers.Default

    @Provides
    @Named(DISPATCHER_IO)
    fun ioDispatcher(): CoroutineDispatcher =
        Dispatchers.IO

    @Provides
    @Named(DISPATCHER_MAIN)
    fun mainDispatcher(): CoroutineDispatcher =
        Dispatchers.Main

    @Provides
    @Named(DEVICE_MODEL)
    fun deviceModel(): String =
        Build.MODEL ?: "unknown"

    @Provides
    @Named(DEVICE_OS_VERSION)
    fun deviceOsVersion(): String =
        Build.VERSION.SDK_INT.toString()

    @Provides
    fun provideAlarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Provides
    fun provideWorkManager(context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    fun provideSonarAnalytics(): SonarAnalytics =
        sonarAnalytics

    companion object {
        const val DISPATCHER_DEFAULT = "DISPATCHER_DEFAULT"
        const val DISPATCHER_IO = "DISPATCHER_IO"
        const val DISPATCHER_MAIN = "DISPATCHER_MAIN"
        const val DEVICE_MODEL = "DEVICE_MODEL"
        const val DEVICE_OS_VERSION = "DEVICE_OS_MODEL"
    }
}
