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
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics
import uk.nhs.nhsx.sonar.android.app.analytics.collectedContactEvents

@ExperimentalCoroutinesApi
class DeleteOutdatedEventsWorkTest {

    private val now = DateTime.parse("2020-04-29T11:08:10Z")
    private val dao = mockk<ContactEventDao>()
    private val analytics = mockk<SonarAnalytics>(relaxed = true)
    private val work = DeleteOutdatedEventsWork(dao, analytics) { now }

    @Test
    fun `doWork - on success()`() = runBlockingTest {
        coEvery { dao.clearOldEvents(any()) } returns Unit
        coEvery { dao.countEvents() } returns 120
        coEvery { dao.countEvents(any(), any()) } returns 26

        val result = work.doWork(3)

        assertThat(result).isEqualTo(Result.success())

        val expectedTimeStamp = DateTime.parse("2020-04-01T00:00:00Z").millis
        val expectedFromTimestamp = now.minusDays(1).withTimeAtStartOfDay().millis
        val expectedToTimestamp = now.withTimeAtStartOfDay().millis

        coVerifyAll {
            dao.clearOldEvents(expectedTimeStamp)
            dao.countEvents()
            dao.countEvents(from = expectedFromTimestamp, to = expectedToTimestamp)
            analytics.trackEvent(collectedContactEvents(yesterday = 26, all = 120))
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
