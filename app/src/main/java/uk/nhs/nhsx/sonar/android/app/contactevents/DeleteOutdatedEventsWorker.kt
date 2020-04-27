/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import uk.nhs.nhsx.sonar.android.app.appComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DeleteOutdatedEventsWorker(
    appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @Inject
    lateinit var contactEventDao: ContactEventDao

    override suspend fun doWork(): Result {
        appComponent.inject(this)
        return DeleteOutdatedEventsWork(contactEventDao).doWork(params.runAttemptCount)
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder().build()

            val request =
                PeriodicWorkRequestBuilder<DeleteOutdatedEventsWorker>(1, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .setInitialDelay(5, TimeUnit.SECONDS)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                    .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
