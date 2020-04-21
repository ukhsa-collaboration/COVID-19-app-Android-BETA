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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.EmberState
import uk.nhs.nhsx.sonar.android.app.status.RecoveryState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.app.status.Symptom

@ExperimentalCoroutinesApi
class DiagnoseCoughViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val stateStorage = mockk<StateStorage>(relaxed = true)

    private val testObserver = mockk<Observer<StateResult>>(relaxed = true)

    private val testDispatcher = TestCoroutineDispatcher()

    private val testSubject =
        DiagnoseCoughViewModel(
            stateStorage
        )

    @Before
    fun setUp() {
        testSubject.observeUserState().observeForever(testObserver)
        DateTimeUtils.setCurrentMillisFixed(10L)
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `initial state is blue then final state is blue`() {
        every { stateStorage.get() } returns DefaultState(DateTime.now())
        testSubject.update(hasTemperature = false, hasCough = false)

        verify {
            testObserver.onChanged(StateResult.Close)
        }
    }

    @Test
    fun `initial state is blue then final state is red`() {
        every { stateStorage.get() } returns DefaultState(DateTime.now())
        testSubject.update(hasTemperature = true, hasCough = false)

        verify {
            testObserver.onChanged(
                StateResult.Review(hasCough = false)
            )
        }
    }

    @Test
    fun `initial state is red then final state Is blue`() {
        val expected = DefaultState(DateTime.now(UTC).plusDays(1))
        every { stateStorage.get() } returns RedState(DateTime.now(UTC), setOf(Symptom.COUGH))
        testSubject.update(hasTemperature = false, hasCough = false)

        verify {
            testObserver.onChanged(
                StateResult.Main(
                    expected
                )
            )
        }
    }

    @Test
    fun `initial state is red then final state is recovery`() {
        val expected = RecoveryState(DateTime.now(UTC).plusDays(1))
        every { stateStorage.get() } returns RedState(DateTime.now(UTC), setOf(Symptom.COUGH))
        testSubject.update(hasTemperature = false, hasCough = true)

        verify {
            testObserver.onChanged(
                StateResult.Main(
                    expected
                )
            )
        }
    }

    @Test
    fun `initial state is red then final state is red`() {
        val expected = RedState(DateTime.now(UTC).plusDays(1), setOf(Symptom.TEMPERATURE))
        every { stateStorage.get() } returns RedState(DateTime.now(UTC), setOf(Symptom.COUGH))
        testSubject.update(hasTemperature = true, hasCough = false)

        verify {
            testObserver.onChanged(
                StateResult.Main(
                    expected
                )
            )
        }
    }

    @Test
    fun `initial state is Amber then final state Is red`() {
        val expected = RedState(DateTime.now(UTC).plusDays(7), setOf(Symptom.TEMPERATURE))
        every { stateStorage.get() } returns EmberState(DateTime.now(UTC))
        testSubject.update(hasTemperature = true, hasCough = false)

        verify {
            testObserver.onChanged(
                StateResult.Main(expected)
            )
        }
    }

    @Test
    fun `initial state is Amber then final state Is Amber`() {
        val expected = EmberState(DateTime.now(UTC))
        every { stateStorage.get() } returns EmberState(DateTime.now(UTC))
        testSubject.update(hasTemperature = false, hasCough = false)

        verify {
            testObserver.onChanged(
                StateResult.Main(expected)
            )
        }
    }

    @After
    fun cleanup() {
        DateTimeUtils.setCurrentMillisSystem()
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}
