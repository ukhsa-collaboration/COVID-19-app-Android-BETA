package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest.MIN_BACKOFF_MILLIS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationWorker.Companion.WAITING_FOR_ACTIVATION_CODE
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class RegistrationManager @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager,
    @Named(AppModule.DISPATCHER_MAIN) private val dispatcher: CoroutineDispatcher
) : CoroutineScope {
    fun register(initialDelaySeconds: Long = 0) {
        Timber.tag("RegistrationUseCase")
            .d("register initialDelaySeconds = $initialDelaySeconds")

        val registrationWorkRequest = createWorkRequest(initialDelaySeconds)

        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            registrationWorkRequest
        )

        handleResult(registrationWorkRequest.id)
    }

    private fun handleResult(workRequestId: UUID) {
        launch {
            workManager.getWorkInfoByIdLiveData(workRequestId)
                .observeForever { workInfo ->
                    handleWorkInfo(workInfo)
                }
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
                scheduleRegisterRetryInOneHour()
            } else {
                BluetoothService.start(context)
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun scheduleRegisterRetryInOneHour() {
        register(ONE_HOUR_IN_SECONDS)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createWorkRequest(initialDelaySeconds: Long): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        return OneTimeWorkRequestBuilder<RegistrationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
    }

    override val coroutineContext: CoroutineContext
        get() = (dispatcher + Job())

    companion object {
        const val WORK_NAME = "registration"
        const val ONE_HOUR_IN_SECONDS = 60L * 60
    }
}
