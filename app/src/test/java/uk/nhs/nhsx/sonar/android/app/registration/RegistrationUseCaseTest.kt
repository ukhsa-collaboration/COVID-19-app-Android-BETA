package uk.nhs.nhsx.sonar.android.app.registration

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runBlockingTest
import net.lachlanmckee.timberjunit.TimberTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.persistence.BluetoothCryptogramProvider
import uk.nhs.nhsx.sonar.android.app.persistence.ID_NOT_REGISTERED
import uk.nhs.nhsx.sonar.android.app.persistence.SonarIdProvider
import uk.nhs.nhsx.sonar.android.client.resident.DeviceConfirmation
import uk.nhs.nhsx.sonar.android.client.resident.Registration
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi
import java.io.IOException

@ExperimentalCoroutinesApi
class RegistrationUseCaseTest {

    private val tokenRetriever = mockk<TokenRetriever>()
    private val residentApi = mockk<ResidentApi>()
    private val activationCodeObserver = mockk<ActivationCodeObserver>()
    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val bluetoothCryptogramProvider = mockk<BluetoothCryptogramProvider>()

    private val confirmation =
        DeviceConfirmation(ACTIVATION_CODE, FIREBASE_TOKEN, DEVICE_MODEL, DEVICE_OS_VERSION)

    private val sut =
        RegistrationUseCase(
            tokenRetriever,
            residentApi,
            activationCodeObserver,
            sonarIdProvider,
            bluetoothCryptogramProvider,
            DEVICE_MODEL,
            DEVICE_OS_VERSION
        )

    @Rule
    @JvmField
    val logAllOnFailuresRule: TimberTestRule = TimberTestRule.logAllWhenTestFails()

    @Before
    fun setUp() {
        Timber.plant(Timber.DebugTree())

        every { sonarIdProvider.getSonarId() } returns ID_NOT_REGISTERED
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { sonarIdProvider.setSonarId(any()) } returns Unit
        coEvery { tokenRetriever.retrieveToken() } returns TokenRetriever.Result.Success(
            FIREBASE_TOKEN
        )
        every { residentApi.register(FIREBASE_TOKEN, any(), any()) } answers {
            secondArg<() -> Unit>().invoke()
        }

        val slot = slot<(String) -> Unit>()
        every { activationCodeObserver.setListener(capture(slot)) } answers {
            slot.captured(ACTIVATION_CODE)
        }
        every { activationCodeObserver.removeListener() } answers {
            nothing
        }

        every { residentApi.confirmDevice(confirmation, any(), any()) } answers {
            secondArg<(Registration) -> Unit>().invoke(Registration(RESIDENT_ID))
        }
    }

    @Test
    fun onSuccessReturnsSuccess() = runBlockingTest {
        val result = sut.register()

        assertThat(RegistrationResult.Success).isEqualTo(result)
    }

    @Test
    fun shouldReturnIfAlreadyRegistered() = runBlockingTest {
        every { sonarIdProvider.hasProperSonarId() } returns true

        val result = sut.register()

        assertThat(RegistrationResult.AlreadyRegistered).isEqualTo(result)
    }

    @Test
    fun retrievesFirebaseToken() = runBlockingTest {
        sut.register()

        coVerify { tokenRetriever.retrieveToken() }
    }

    @Test
    fun onTokenRetrievalFailureReturnFailure() = runBlockingTest {
        coEvery { tokenRetriever.retrieveToken() } returns TokenRetriever.Result.Failure(null)

        val result = sut.register()

        assertThat(result).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test
    fun registersDevice() = runBlockingTest {
        sut.register()

        verify { residentApi.register(FIREBASE_TOKEN, any(), any()) }
    }

    @Test
    fun onDeviceRegistrationFailureReturnsFailure() = runBlockingTest {
        every { residentApi.register(any(), any(), any()) } answers {
            thirdArg<(Exception) -> Unit>().invoke(IOException())
        }

        val result = sut.register()

        assertThat(result).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test
    fun listensActivationCodeObserver() = runBlockingTest {
        sut.register()

        verify { activationCodeObserver.setListener(any()) }
    }

    @Test
    fun onActivationCodeTimeoutReturnsFailure() = runBlockingTest {
        every { activationCodeObserver.setListener(any()) } returns Unit

        val result = async {
            sut.register()
        }
        advanceTimeBy(25_000)

        assertThat(result.getCompleted()).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test(expected = IllegalStateException::class)
    fun onActivationCodeTimeoutShouldWaitFor20seconds() = runBlockingTest {
        every { activationCodeObserver.setListener(any()) } returns Unit

        val result = async {
            sut.register()
        }
        advanceTimeBy(19_000)
        result.getCompleted()
    }

    @Test
    fun registersResident() = runBlockingTest {
        sut.register()

        verify { residentApi.confirmDevice(confirmation, any(), any()) }
    }

    @Test
    fun onResidentRegistrationFailureReturnsFailure() = runBlockingTest {
        every { residentApi.confirmDevice(confirmation, any(), any()) } answers {
            arg<(Exception) -> Unit>(3).invoke(IOException())
        }

        val result = sut.register()

        assertThat(result).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test
    fun onSuccessSavesSonarId() = runBlockingTest {
        sut.register()

        verify { sonarIdProvider.setSonarId(RESIDENT_ID) }
    }

    companion object {
        const val FIREBASE_TOKEN = "::firebase token::"
        const val ACTIVATION_CODE = "::activation code::"
        const val DEVICE_MODEL = "::device model::"
        const val DEVICE_OS_VERSION = "24"
        const val RESIDENT_ID = "::resident id::"
    }
}
