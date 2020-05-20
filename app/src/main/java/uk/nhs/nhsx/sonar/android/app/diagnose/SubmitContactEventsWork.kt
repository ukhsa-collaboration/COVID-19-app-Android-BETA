/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.workDataOf
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import uk.nhs.nhsx.sonar.android.app.contactevents.CoLocationDataProvider
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationApi
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationData
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import javax.inject.Inject

class SubmitContactEventsWork @Inject constructor(
    private val coLocationApi: CoLocationApi,
    private val coLocationDataProvider: CoLocationDataProvider,
    private val sonarIdProvider: SonarIdProvider
) {

    companion object {
        const val SYMPTOMS_DATE = "SYMPTOMS_DATE"
        const val SYMPTOMS = "SYMPTOMS"

        fun data(symptomsDate: LocalDate, symptoms: List<Symptom>): Data {
            val symptomsDateUtcIsoFormat: String =
                symptomsDate.toDateTime(LocalTime.now(), DateTimeZone.UTC).toUtcIsoFormat()
            val symptomsArray = symptoms.map { it.value }.toTypedArray()

            return workDataOf(SYMPTOMS_DATE to symptomsDateUtcIsoFormat, SYMPTOMS to symptomsArray)
        }
    }

    suspend fun doWork(data: Data): Result =
        runCatching {
            val symptomsTimestamp = data.getString(SYMPTOMS_DATE)!!
            val symptomsArray = data.getStringArray(SYMPTOMS)!!
            val symptoms = symptomsArray.mapNotNull { Symptom.fromValue(it) }

            val coLocationData = CoLocationData(
                sonarId = sonarIdProvider.get(),
                symptomsTimestamp = symptomsTimestamp,
                symptoms = symptoms,
                contactEvents = coLocationDataProvider.getEvents()
            )

            coLocationApi.save(coLocationData).toCoroutineUnsafe()
            coLocationDataProvider.clearData()

            Result.success()
        }.getOrElse {
            Result.retry()
        }
}
