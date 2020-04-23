package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest.MIN_BACKOFF_MILLIS
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RegistrationManager @Inject constructor(
    val context: Context,
    @Named(AppModule.DISPATCHER_MAIN) val dispatcher: CoroutineDispatcher
) {
    fun tryRegister(initialDelaySeconds: Long = 0) {
        Timber.tag("RegistrationUseCase")
            .d("tryRegister initialDelaySeconds = $initialDelaySeconds")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val registrationWorkRequest = OneTimeWorkRequestBuilder<RegistrationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("registration", ExistingWorkPolicy.REPLACE, registrationWorkRequest)

        GlobalScope.launch {
            withContext(dispatcher) {
                WorkManager.getInstance(context).getWorkInfoByIdLiveData(registrationWorkRequest.id)
                    .observeForever { workInfo ->
                        if (workInfo == null) {
                            return@observeForever
                        }
                        Timber.tag("RegistrationUseCase").d("workInfo.state = ${workInfo.state}")
                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                            val waitingForActivationCode = workInfo.outputData.getBoolean(
                                RegistrationWorker.WAITING_FOR_ACTIVATION_CODE,
                                false
                            )
                            if (waitingForActivationCode) {
                                tryRegister(60 * 60 * 1000)
                            } else {
                                BluetoothService.start(context)
                            }
                        } else if (workInfo.state == WorkInfo.State.FAILED) {
                            val activationCodeNotValid = workInfo.outputData.getBoolean(
                                RegistrationWorker.ACTIVATION_CODE_NOT_VALID,
                                false
                            )
                            if (activationCodeNotValid) {
                                tryRegister()
                            }
                        }
                    }
            }
        }
    }
}

class RegistrationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @Inject
    lateinit var registrationUseCase: RegistrationUseCase

    override suspend fun doWork(): Result {
        appComponent.inject(this)
        val result = registrationUseCase.register()
        Timber.tag("RegistrationUseCase").d("doWork result = $result")
        return when (result) {
            RegistrationResult.Success, RegistrationResult.AlreadyRegistered -> Result.success()
            is RegistrationResult.Failure -> Result.retry()
            is RegistrationResult.ActivationCodeNotValidFailure -> {
                val outputData = workDataOf(ACTIVATION_CODE_NOT_VALID to true)
                Result.failure(outputData)
            }
            RegistrationResult.WaitingForActivationCode -> {
                val outputData = workDataOf(WAITING_FOR_ACTIVATION_CODE to true)
                Result.success(outputData)
            }
        }
    }

    companion object {
        const val ACTIVATION_CODE_NOT_VALID = "ACTIVATION_CODE_NOT_VALID"
        const val WAITING_FOR_ACTIVATION_CODE = "WAITING_FOR_ACTIVATION_CODE"
    }
}
