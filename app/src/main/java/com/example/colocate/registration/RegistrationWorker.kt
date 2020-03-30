package com.example.colocate.registration

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.colocate.ColocateApplication
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class RegistrationWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        val applicationComponent = (context as ColocateApplication).applicationComponent
        val result = applicationComponent.registrationUseCase().register()
        Timber.d("Registration result: $result")
        when (result) {
            RegistrationResult.Success -> Result.success()
            is RegistrationResult.Failure -> Result.retry()
            RegistrationResult.AlreadyRegistered -> Result.success()
        }
    }
}
