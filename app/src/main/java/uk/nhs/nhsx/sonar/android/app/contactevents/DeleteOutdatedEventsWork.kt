package uk.nhs.nhsx.sonar.android.app.contactevents

import androidx.work.ListenableWorker.Result
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber

class DeleteOutdatedEventsWork(
    private val contactEventDao: ContactEventDao,
    private val dateTimeProvider: () -> DateTime = { DateTime.now(DateTimeZone.UTC) }
) {

    suspend fun doWork(attempts: Int): Result {
        Timber.d("Started to delete outdated events... ")

        if (attempts > 3) {
            Timber.d("Giving up after $attempts attempts ")
            return Result.failure()
        }

        val timestamp = dateTimeProvider().minusDays(28).withTimeAtStartOfDay()

        Timber.d("Deleting all events before $timestamp")

        return try {
            contactEventDao.clearOldEvents(timestamp.millis)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete events")
            Result.retry()
        }
    }
}
