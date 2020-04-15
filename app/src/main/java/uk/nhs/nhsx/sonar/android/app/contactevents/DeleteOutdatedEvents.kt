package uk.nhs.nhsx.sonar.android.app.contactevents

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DeleteOutdatedEvents(
    appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @Inject
    protected lateinit var contactEventDao: ContactEventDao

    override suspend fun doWork(): Result {
        appComponent.inject(this)

        Timber.d("Started to delete outdated events... ")

        val attempts = params.runAttemptCount
        if (attempts > 3) {
            Timber.d("Giving up after $attempts attempts ")
            return Result.failure()
        }

        val timestamp = DateTime.now(DateTimeZone.UTC).minusDays(28).toUtcIsoFormat()

        Timber.d("Deleting all events before $timestamp")
        return try {
            contactEventDao.clearOldEvents(timestamp)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete events")
            Result.retry()
        }
    }

    companion object {

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().build()

            val request =
                PeriodicWorkRequestBuilder<DeleteOutdatedEvents>(1, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .setInitialDelay(5, TimeUnit.SECONDS)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                    .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
