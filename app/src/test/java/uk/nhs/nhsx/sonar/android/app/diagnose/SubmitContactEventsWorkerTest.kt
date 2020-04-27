package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.contactevents.CoLocationDataProvider
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationApi
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationData
import uk.nhs.nhsx.sonar.android.app.http.Promise
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat

@ExperimentalCoroutinesApi
class SubmitContactEventsWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val appComponent = mockk<ApplicationComponent>(relaxed = true)

    private val contactEventDao = mockk<ContactEventDao>()

    private val coLocationApi = mockk<CoLocationApi>()

    private val coLocationDataProvider = mockk<CoLocationDataProvider>()

    private val sonarIdProvider = mockk<SonarIdProvider>()

    private var symptomDate = DateTime.now().toUtcIsoFormat()

    private val testSubject = SubmitContactEventsWorker(context, workerParams)

    @Before
    fun setUp() {
        mockkStatic("uk.nhs.nhsx.sonar.android.app.ColocateApplicationKt")
        every { testSubject.appComponent } returns appComponent

        testSubject.coLocationApi = coLocationApi
        testSubject.contactEventDao = contactEventDao
        testSubject.coLocationDataProvider = coLocationDataProvider
        testSubject.sonarIdProvider = sonarIdProvider

        val data = workDataOf(SYMPTOMS_DATE to symptomDate)

        every { workerParams.inputData } returns data
        every { sonarIdProvider.getSonarId() } returns SONAR_ID
        coEvery { coLocationDataProvider.clearData() } returns Unit
    }

    @Test
    fun `verify upload contact events is called`() = runBlockingTest {
        coEvery { coLocationDataProvider.getEvents() } returns emptyList()

        val coLocationData = CoLocationData(
            SONAR_ID,
            symptomDate,
            emptyList()
        )

        testSubject.doWork()

        verify {
            coLocationApi.save(eq(coLocationData))
        }
    }

    @Test
    fun `verify successful completion of worker`() = runBlockingTest {
        coEvery { coLocationDataProvider.getEvents() } returns emptyList()

        val coLocationData = CoLocationData(
            SONAR_ID,
            symptomDate,
            emptyList()
        )

        coEvery { coLocationApi.save(coLocationData) } returns uploadEventsDeferred().promise

        val result = testSubject.doWork()

        Assertions.assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }

    @Test
    fun `verify on error returns retry`() = runBlockingTest {
        every { sonarIdProvider.getSonarId() } returns SONAR_ID
        coEvery { coLocationDataProvider.getEvents() } throws Exception()
        coEvery { coLocationDataProvider.clearData() } returns Unit

        val coLocationData = CoLocationData(
            SONAR_ID,
            symptomDate,
            emptyList()
        )

        coEvery { coLocationApi.save(coLocationData) } returns uploadEventsDeferred().promise

        val result = testSubject.doWork()

        Assertions.assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
    }

    private fun uploadEventsDeferred() =
        Promise.Deferred<Unit>().apply {
            resolve(Unit)
        }

    companion object {
        private const val SYMPTOMS_DATE = "SYMPTOMS_DATE"
        private const val SONAR_ID = "sonar_id"
    }
}
