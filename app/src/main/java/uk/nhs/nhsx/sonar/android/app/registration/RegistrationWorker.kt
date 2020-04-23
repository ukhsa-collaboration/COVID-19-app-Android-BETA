package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.appComponent
import javax.inject.Inject

class RegistrationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @Inject
    lateinit var registrationUseCase: RegistrationUseCase

    override suspend fun doWork(): Result {
        appComponent.inject(this)
        val result = registrationUseCase.register()
        Timber.tag("RegistrationUseCase").d("doWork result = $result")
        return when (result) {
            RegistrationResult.Success -> Result.success()
            RegistrationResult.Error -> Result.retry()
            RegistrationResult.WaitingForActivationCode -> {
                val outputData =
                    workDataOf(WAITING_FOR_ACTIVATION_CODE to true)
                Result.success(outputData)
            }
        }
    }

    companion object {
        const val WAITING_FOR_ACTIVATION_CODE = "WAITING_FOR_ACTIVATION_CODE"
    }
}
