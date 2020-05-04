/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import androidx.work.ListenableWorker.Result
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics
import uk.nhs.nhsx.sonar.android.app.analytics.collectedContactEvents

class DeleteOutdatedEventsWork(
    private val contactEventDao: ContactEventDao,
    private val analytics: SonarAnalytics,
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
            doAnalytics()
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete events")
            Result.retry()
        }
    }

    private suspend fun doAnalytics() {
        val now = dateTimeProvider()

        val from = now.minusDays(1).withTimeAtStartOfDay().millis
        val to = now.withTimeAtStartOfDay().millis
        val yesterday = contactEventDao.countEvents(from, to)

        val all = contactEventDao.countEvents()
        analytics.trackEvent(collectedContactEvents(yesterday, all))
    }
}
