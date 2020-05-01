package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.work.ListenableWorker
import androidx.work.workDataOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.contactevents.CoLocationDataProvider
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationApi
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationData
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationEvent
import uk.nhs.nhsx.sonar.android.app.http.Promise.Deferred
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import java.util.Base64
import kotlin.random.Random

@ExperimentalCoroutinesApi
class SubmitContactEventsWorkTest {

    private val coLocationApi = mockk<CoLocationApi>()
    private val coLocationDataProvider = mockk<CoLocationDataProvider>()
    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val work =
        SubmitContactEventsWork(coLocationApi, coLocationDataProvider, sonarIdProvider)

    private val events: List<CoLocationEvent> = listOf(
        CoLocationEvent(
            "001",
            Base64.getEncoder().encodeToString(byteArrayOf(0, 10.toByte())),
            listOf(0, 2),
            "2s ago",
            10,
            1.toByte(),
            2.toByte(),
            18242.toShort(),
            5,
            Base64.getEncoder().encodeToString(Random.nextBytes(16))
        )
    )

    @Test
    fun `test doWork()`() = runBlockingTest {
        val residentId = "80baf81b-8afd-47e9-9915-50691525c910"
        val symptomsDate = "2020-04-24T11:04:10Z"
        val saveDeferred = Deferred<Unit>()

        every { sonarIdProvider.getSonarId() } returns residentId
        coEvery { coLocationDataProvider.getEvents() } returns events
        every { coLocationApi.save(any()) } returns saveDeferred.promise
        coEvery { coLocationDataProvider.clearData() } returns Unit

        saveDeferred.resolve(Unit)

        val result = work.doWork(workDataOf("SYMPTOMS_DATE" to symptomsDate))

        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        verify { coLocationApi.save(CoLocationData(residentId, symptomsDate, events)) }
        coVerify { coLocationDataProvider.clearData() }
    }

    @Test
    fun `test doWork() on save failure`() = runBlockingTest {
        val residentId = "80baf81b-8afd-47e9-9915-50691525c910"
        val symptomsDate = "2020-04-24T11:04:10Z"
        val saveDeferred = Deferred<Unit>()

        every { sonarIdProvider.getSonarId() } returns residentId
        coEvery { coLocationDataProvider.getEvents() } returns events
        every { coLocationApi.save(any()) } returns saveDeferred.promise

        saveDeferred.fail(Exception("Failed to save"))

        val result = work.doWork(workDataOf("SYMPTOMS_DATE" to symptomsDate))

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())

        coVerify(exactly = 0) { coLocationDataProvider.clearData() }
    }
}
