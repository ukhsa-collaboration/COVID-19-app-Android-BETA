package uk.nhs.nhsx.sonar.android.app.referencecode

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest.MIN_BACKOFF_MILLIS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class ReferenceCodeWorkLauncher @Inject constructor(
    private val workManager: WorkManager,
    @Named(AppModule.DISPATCHER_MAIN) private val dispatcher: CoroutineDispatcher
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = (dispatcher + Job())

    fun launchWork() {
        launch {
            workManager.enqueueUniqueWork(
                REFERENCE_CODE_WORK,
                ExistingWorkPolicy.KEEP,
                createWorkRequest()
            )
        }
    }

    private fun createWorkRequest(): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        return OneTimeWorkRequestBuilder<ReferenceCodeWorker>()
            .setConstraints(constraints)
            .setInitialDelay(0, SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_MILLIS, MILLISECONDS)
            .build()
    }

    companion object {
        const val REFERENCE_CODE_WORK = "FetchReferenceCode"
    }
}
