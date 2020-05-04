/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uk.nhs.nhsx.sonar.android.app.appComponent
import javax.inject.Inject

class RegistrationWorker(appContext: Context, private val workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @Inject
    lateinit var registrationUseCase: RegistrationUseCase

    override suspend fun doWork(): Result {
        appComponent.inject(this)
        return RegistrationWork(registrationUseCase).doWork(workerParams.inputData)
    }

    companion object {
        const val WAITING_FOR_ACTIVATION_CODE = "WAITING_FOR_ACTIVATION_CODE"
    }
}
