/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.contactevents.CoLocationDataProvider
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationApi
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationData
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import java.lang.Exception
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class SubmitContactEventsWorker(
    appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @Inject
    protected lateinit var contactEventDao: ContactEventDao

    @Inject
    protected lateinit var coLocationApi: CoLocationApi

    @Inject
    protected lateinit var coLocationDataProvider: CoLocationDataProvider

    @Inject
    protected lateinit var sonarIdProvider: SonarIdProvider

    override suspend fun doWork(): Result {
        appComponent.inject(this)
        Timber.d("Started uploading contact events... ")

        try {
            val symptomsTimestamp = params.inputData.getString(SYMPTOMS_DATE)!!

            val coLocationData = CoLocationData(
                sonarId = sonarIdProvider.getSonarId(),
                symptomsTimestamp = symptomsTimestamp,
                contactEvents = coLocationDataProvider.getEvents()
            )

            uploadContactEvents(coLocationData)

            coLocationDataProvider.clearData()
            return Result.success()
        } catch (e: Exception) {
            Timber.e(e)
            return Result.retry()
        }
    }

    private suspend fun uploadContactEvents(coLocationData: CoLocationData) {
        return suspendCoroutine { continuation ->
            coLocationApi
                .save(coLocationData)
                .onSuccess {
                    Timber.d("Success")
                    continuation.resumeWith(kotlin.Result.success(Unit))
                }
                .onError {
                    Timber.e("Error: $it")
                    continuation.resumeWith(kotlin.Result.failure(it))
                }
        }
    }

    companion object {
        private const val SYMPTOMS_DATE = "SYMPTOMS_DATE"

        fun schedule(context: Context, symptomsDate: LocalDate) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val symptomsDateUtcIsoFormat: String =
                symptomsDate.toDateTime(LocalTime.now(), DateTimeZone.UTC).toUtcIsoFormat()

            val data = workDataOf(SYMPTOMS_DATE to symptomsDateUtcIsoFormat)

            val request =
                OneTimeWorkRequestBuilder<SubmitContactEventsWorker>()
                    .setConstraints(constraints)
                    .setInputData(data)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                    .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}