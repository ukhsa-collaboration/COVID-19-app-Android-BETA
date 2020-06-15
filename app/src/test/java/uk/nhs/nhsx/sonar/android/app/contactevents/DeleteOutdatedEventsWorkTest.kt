/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import androidx.work.ListenableWorker.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test

@ExperimentalCoroutinesApi
class DeleteOutdatedEventsWorkTest {

    private val now = DateTime.parse("2020-04-29T11:08:10Z")
    private val dao = mockk<ContactEventDao>()
    private val work = DeleteOutdatedEventsWork(dao) { now }

    @Test
    fun `doWork - on success()`() = runBlockingTest {
        coEvery { dao.clearOldEvents(any()) } returns Unit

        val result = work.doWork(3)

        assertThat(result).isEqualTo(Result.success())

        val expectedTimeStamp = DateTime.parse("2020-04-01T00:00:00Z").millis

        coVerifyAll {
            dao.clearOldEvents(expectedTimeStamp)
        }
    }

    @Test
    fun `doWork - when attempts exceed 3`() = runBlockingTest {
        val result = work.doWork(4)

        assertThat(result).isEqualTo(Result.failure())

        coVerify(exactly = 0) { dao.clearOldEvents(any()) }
    }

    @Test
    fun `doWork - when clearOldEvents fails`() = runBlockingTest {
        coEvery { dao.clearOldEvents(any()) } throws Exception("DAO failed")

        val result = work.doWork(1)

        assertThat(result).isEqualTo(Result.retry())
    }
}
