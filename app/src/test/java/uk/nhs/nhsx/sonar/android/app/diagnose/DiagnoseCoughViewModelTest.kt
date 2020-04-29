package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.EmberState
import uk.nhs.nhsx.sonar.android.app.status.RecoveryState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

@ExperimentalCoroutinesApi
class DiagnoseCoughViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val stateStorage = mockk<StateStorage>(relaxed = true)
    private val testObserver = mockk<Observer<StateResult>>(relaxed = true)
    private val testDispatcher = TestCoroutineDispatcher()
    private val testSubject = DiagnoseCoughViewModel(stateStorage)

    @Before
    fun setUp() {
        testSubject.observeUserState().observeForever(testObserver)
        DateTimeUtils.setCurrentMillisFixed(0L)
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `initial state is blue then final state is blue`() {
        every { stateStorage.get() } returns DefaultState()
        testSubject.update(hasTemperature = false, hasCough = false)

        verify {
            testObserver.onChanged(StateResult.Close)
        }
    }

    @Test
    fun `initial state is blue then final state is red`() {
        every { stateStorage.get() } returns DefaultState()
        testSubject.update(hasTemperature = true, hasCough = false)

        verify {
            testObserver.onChanged(
                StateResult.Review(nonEmptySetOf(TEMPERATURE))
            )
        }
    }

    @Test
    fun `initial state is red then final state Is blue`() {
        val expected = DefaultState()
        every { stateStorage.get() } returns RedState(DateTime.now(UTC), nonEmptySetOf(COUGH))
        testSubject.update(hasTemperature = false, hasCough = false)

        verify {
            testObserver.onChanged(
                StateResult.Main(expected)
            )
        }
    }

    @Test
    fun `initial state is red then final state is recovery`() {
        val expected = RecoveryState()
        every { stateStorage.get() } returns RedState(DateTime.now(UTC), nonEmptySetOf(COUGH))
        testSubject.update(hasTemperature = false, hasCough = true)

        verify {
            testObserver.onChanged(
                StateResult.Main(expected)
            )
        }
    }

    @Test
    fun `initial state is red then final state is red`() {
        val tomorrowSevenAm = LocalDate.now()
            .plusDays(1)
            .toDateTime(LocalTime("7:00:00"))
            .toDateTime(UTC)

        val expected = RedState(tomorrowSevenAm, nonEmptySetOf(TEMPERATURE))
        every { stateStorage.get() } returns RedState(DateTime.now(UTC), nonEmptySetOf(COUGH))
        testSubject.update(hasTemperature = true, hasCough = false)

        verify {
            testObserver.onChanged(
                StateResult.Main(expected)
            )
        }
    }

    @Test
    fun `initial state is Amber then final state Is red`() {
        RedState(DateTime.now(UTC).plusDays(7), nonEmptySetOf(TEMPERATURE))
        every { stateStorage.get() } returns EmberState(DateTime.now(UTC))
        testSubject.update(hasTemperature = true, hasCough = false)

        verify {
            testObserver.onChanged(StateResult.Review(nonEmptySetOf(TEMPERATURE)))
        }
    }

    @Test
    fun `initial state is Amber then final state Is Amber`() {
        every { stateStorage.get() } returns EmberState(DateTime.now(UTC))
        testSubject.update(hasTemperature = false, hasCough = false)

        verify {
            testObserver.onChanged(StateResult.Close)
        }
    }

    @After
    fun cleanup() {
        DateTimeUtils.setCurrentMillisSystem()
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}
