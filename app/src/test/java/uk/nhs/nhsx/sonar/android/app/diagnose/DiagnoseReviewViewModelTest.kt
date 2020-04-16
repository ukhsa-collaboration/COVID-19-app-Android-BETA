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
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.contactevents.CoLocationDataProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationApi
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationData
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationEvent

class DiagnoseReviewViewModelTest {

    private val coLocationApi = mockk<CoLocationApi>(relaxed = true)
    private val coLocationDataProvider = mockk<CoLocationDataProvider>()
    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val testSubject = DiagnoseReviewViewModel(
        coLocationApi,
        Dispatchers.Unconfined,
        coLocationDataProvider
    )

    companion object {
        private const val RESIDENT_ID = "80baf81b-8afd-47e9-9915-50691525c910"
    }

    @Test
    fun onUploadContactEvents() {
        runBlocking {
            val events = listOf(
                CoLocationEvent("001", listOf(-10, 0), "2s ago", 10),
                CoLocationEvent("002", listOf(-10, -10, 10), "yesterday", 120)
            )
            val coLocationData = CoLocationData(RESIDENT_ID, events)
            coEvery { coLocationDataProvider.getData() } returns coLocationData
            every { sonarIdProvider.getSonarId() } returns RESIDENT_ID

            testSubject.uploadContactEvents()

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
