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
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.contactevents.DeleteOutdatedEventsWork
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeWaitTime
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper
import uk.nhs.nhsx.sonar.android.app.util.NotificationManagerHelper
import java.util.concurrent.TimeUnit
import javax.inject.Named

@Module
class AppModule(
    private val applicationContext: Context,
    private val locationHelper: LocationHelper,
    private val notificaitonManagerHelper: NotificationManagerHelper,
    private val activationCodeWaitTime: ActivationCodeWaitTime = ActivationCodeWaitTime(
        1,
        TimeUnit.HOURS
    )
) {

    @Provides
    fun provideContext() = applicationContext

    @Provides
    fun provideLocationHelper() = locationHelper

    @Provides
    fun provideNotificaitonManagerHelper() = notificaitonManagerHelper

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
    fun activationCodeWaitTime(): ActivationCodeWaitTime =
        activationCodeWaitTime

    @Provides
    fun provideAlarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Provides
    fun provideWorkManager(context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    fun provideDeleteOutdatedEventsWork(dao: ContactEventDao): DeleteOutdatedEventsWork =
        DeleteOutdatedEventsWork(dao)

    companion object {
        const val DISPATCHER_DEFAULT = "DISPATCHER_DEFAULT"
        const val DISPATCHER_IO = "DISPATCHER_IO"
        const val DISPATCHER_MAIN = "DISPATCHER_MAIN"
        const val DEVICE_MODEL = "DEVICE_MODEL"
        const val DEVICE_OS_VERSION = "DEVICE_OS_MODEL"
        const val REGISTRATION_ACTIVATION_CODE_WAIT_TIME = "REGISTRATION_ACTIVATION_CODE_WAIT_TIME"
    }
}
