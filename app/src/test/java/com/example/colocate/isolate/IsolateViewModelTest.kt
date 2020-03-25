/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.isolate

import com.example.colocate.network.convert
import com.example.colocate.persistence.ContactEvent
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ResidentIdProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationApi
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationData

class IsolateViewModelTest {

    private lateinit var testSubject: IsolateViewModel

    @Mock
    private lateinit var colocationApi: CoLocationApi

    @Mock
    private lateinit var contactEventDao: ContactEventDao

    @Mock
    private lateinit var residentIdProvider: ResidentIdProvider

    companion object {
        private val RESIDENT_ID = "80baf81b-8afd-47e9-9915-50691525c910"
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        testSubject = IsolateViewModel(
            colocationApi,
            contactEventDao,
            Dispatchers.Unconfined,
            residentIdProvider
        )
    }

    @Test
    fun onNotifyCallsColocationApi() {
        runBlocking {

            whenever(contactEventDao.getAll()).thenReturn(getContentEvents())

            whenever(residentIdProvider.getResidentId()).thenReturn(RESIDENT_ID)

            testSubject.onNotifyClick()

            val argumentCaptor = argumentCaptor<CoLocationData>()

            verify(colocationApi).save(
                argumentCaptor.capture(), any(), any()
            )

            assertEquals(
                RESIDENT_ID,
                argumentCaptor.firstValue.residentId
            )
            val expectedValues = convert(getContentEvents())

            assertEquals(
                expectedValues.toString(),
                argumentCaptor.firstValue.events.toString()
            )
        }
    }

    private fun getContentEvents() = listOf(
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
