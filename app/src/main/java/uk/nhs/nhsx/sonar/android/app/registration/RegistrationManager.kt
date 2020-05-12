/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest.MIN_BACKOFF_MILLIS
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationWorker.Companion.WAITING_FOR_ACTIVATION_CODE
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class RegistrationManager @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager,
    @Named(AppModule.DISPATCHER_MAIN) private val dispatcher: CoroutineDispatcher,
    private val activationCodeWaitTime: ActivationCodeWaitTime
) : CoroutineScope {

    private var previousWorkInfoLiveData: LiveData<WorkInfo>? = null
    private val observer = Observer<WorkInfo> { workInfo -> handleWorkInfo(workInfo) }

    fun register(
        initialDelayValue: Long = 0,
        initialDelayTimeUnit: TimeUnit = TimeUnit.SECONDS,
        activationCodeTimedOut: Boolean = false
    ) {
        // Need it for the thread confinement and LiveData can be observed on the Main Thread only
        launch {
            Timber.tag("RegistrationUseCase")
                .d("register initialDelaySeconds = $initialDelayValue")

            val registrationWorkRequest =
                createWorkRequest(initialDelayValue, initialDelayTimeUnit, activationCodeTimedOut)

            workManager.enqueueUniqueWork(
                REGISTRATION_WORK,
                ExistingWorkPolicy.REPLACE,
                registrationWorkRequest
            )

            previousWorkInfoLiveData?.removeObserver(observer)
            val workInfoLiveData = workManager.getWorkInfoByIdLiveData(registrationWorkRequest.id)
            workInfoLiveData.observeForever(observer)
            previousWorkInfoLiveData = workInfoLiveData
        }
    }

    private fun handleWorkInfo(workInfo: WorkInfo?) {
        if (workInfo == null) {
            return
        }
        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
            val waitingForActivationCode =
                workInfo.outputData.getBoolean(WAITING_FOR_ACTIVATION_CODE, false)

            if (waitingForActivationCode) {
                scheduleRegisterRetryAfterActivationCodeWaitTimeExpires()
            } else {
                BluetoothService.start(context)
            }
        }
    }

    private fun scheduleRegisterRetryAfterActivationCodeWaitTimeExpires() {
        register(
            activationCodeWaitTime.timeDelay,
            activationCodeWaitTime.timeUnit,
            activationCodeTimedOut = true
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createWorkRequest(
        initialDelaySeconds: Long,
        initialDelayTimeUnit: TimeUnit,
        activationCodeTimedOut: Boolean
    ): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        return OneTimeWorkRequestBuilder<RegistrationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(initialDelaySeconds, initialDelayTimeUnit)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setInputData(
                workDataOf(
                    ACTIVATION_CODE_TIMED_OUT to activationCodeTimedOut
                )
            )
            .build()
    }

    override val coroutineContext: CoroutineContext
        get() = (dispatcher + Job())

    companion object {
        const val REGISTRATION_WORK = "registration"
        const val ACTIVATION_CODE_TIMED_OUT = "activationCodeTimedOut"
    }
}
