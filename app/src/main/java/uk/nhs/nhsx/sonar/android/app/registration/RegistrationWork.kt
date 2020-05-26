/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.workDataOf
import timber.log.Timber
import javax.inject.Inject

class RegistrationWork @Inject constructor(private val registrationUseCase: RegistrationUseCase) {

    suspend fun doWork(inputData: Data): ListenableWorker.Result {
        val result = registrationUseCase.register(inputData)
        Timber.tag("RegistrationUseCase").d("doWork result = $result")

        return when (result) {
            RegistrationResult.Success -> ListenableWorker.Result.success()
            RegistrationResult.Error -> ListenableWorker.Result.retry()
            RegistrationResult.WaitingForActivationCode -> {
                val outputData =
                    workDataOf(RegistrationWorker.WAITING_FOR_ACTIVATION_CODE to true)
                ListenableWorker.Result.success(outputData)
            }
        }
    }
}
