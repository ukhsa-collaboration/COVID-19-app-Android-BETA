/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.contactevents.CoLocationDataProvider
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationApi
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationData
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationEvent
import uk.nhs.nhsx.sonar.android.app.diagnose.review.DiagnoseReviewViewModel
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat

class DiagnoseReviewViewModelTest {

    private val coLocationApi = mockk<CoLocationApi>(relaxed = true)
    private val coLocationDataProvider = mockk<CoLocationDataProvider>()
    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val testSubject =
        DiagnoseReviewViewModel(
            Dispatchers.Unconfined,
            coLocationApi,
            coLocationDataProvider,
            sonarIdProvider
        )

    companion object {
        private const val RESIDENT_ID = "80baf81b-8afd-47e9-9915-50691525c910"
    }

    @Test
    fun onUploadContactEvents() {
        val symptomsDate = DateTime.now(DateTimeZone.UTC)

        runBlocking {
            val events = listOf(
                CoLocationEvent(encryptedRemoteContactId = "001", rssiValues = listOf(-10, 0), rssiOffsets = listOf(0, 2), timestamp = "2s ago", duration = 10),
                CoLocationEvent(encryptedRemoteContactId = "002", rssiValues = listOf(-10, -10, 10), rssiOffsets = listOf(0, 100, 2000), timestamp = "yesterday", duration = 120)
            )
            val coLocationData = CoLocationData(RESIDENT_ID, symptomsDate.toUtcIsoFormat(), events)
            coEvery { coLocationDataProvider.getEvents() } returns events
            every { sonarIdProvider.getSonarId() } returns RESIDENT_ID

            testSubject.uploadContactEvents(symptomsDate)

            verify {
                coLocationApi.save(eq(coLocationData))
            }
        }
    }

    @Test
    fun onClearContactEvents() {
        runBlocking {
            every { sonarIdProvider.getSonarId() } returns RESIDENT_ID
            coEvery { coLocationDataProvider.clearData() } returns Unit

            testSubject.clearContactEvents()

            coVerify {
                coLocationDataProvider.clearData()
            }
        }
    }
}
