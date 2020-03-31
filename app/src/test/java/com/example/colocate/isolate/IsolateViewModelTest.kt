/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.isolate

import com.example.colocate.network.convert
import com.example.colocate.persistence.ContactEvent
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ResidentIdProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationApi
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationData

class IsolateViewModelTest {

    private val coLocationApi = mockk<CoLocationApi>(relaxed = true)
    private val contactEventDao = mockk<ContactEventDao>()
    private val residentIdProvider = mockk<ResidentIdProvider>()
    private val testSubject = IsolateViewModel(
        coLocationApi,
        contactEventDao,
        Dispatchers.Unconfined,
        residentIdProvider
    )

    companion object {
        private const val RESIDENT_ID = "80baf81b-8afd-47e9-9915-50691525c910"
    }

    @Test
    fun onNotifyCallsCoLocationApi() {
        runBlocking {
            coEvery { contactEventDao.getAll() } returns contentEvents
            every { residentIdProvider.getResidentId() } returns RESIDENT_ID

            testSubject.onNotifyClick()

            val dataSlot = slot<CoLocationData>()
            verify {
                coLocationApi.save(capture(dataSlot), any(), any())
            }

            val coLocationData = dataSlot.captured
            val expectedEvents = convert(contentEvents)
            assertThat(RESIDENT_ID).isEqualTo(coLocationData.residentId)
            assertThat(expectedEvents.toString()).isEqualTo(coLocationData.events.toString())
        }
    }

    private val contentEvents = listOf(
        ContactEvent(
            id = 1L,
            remoteContactId = "remote",
            rssi = 1,
            timestamp = "124325432"
        ),
        ContactEvent(
            id = 2L,
            remoteContactId = "remote",
            rssi = 1,
            timestamp = "124325432"
        )
    )
}
