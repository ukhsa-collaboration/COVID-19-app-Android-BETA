package com.example.colocate.registration

import com.example.colocate.persistence.ID_NOT_REGISTERED
import com.example.colocate.persistence.ResidentIdProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import net.lachlanmckee.timberjunit.TimberTestRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.client.resident.Registration
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi
import java.io.IOException

@ExperimentalCoroutinesApi
class RegistrationUseCaseTest {

    private val tokenRetriever = mockk<TokenRetriever>()
    private val residentApi = mockk<ResidentApi>()
    private val activationCodeObserver = mockk<ActivationCodeObserver>()
    private val residentIdProvider = mockk<ResidentIdProvider>()

    private val sut =
        RegistrationUseCase(tokenRetriever, residentApi, activationCodeObserver, residentIdProvider)

    @Rule
    @JvmField
    val logAllOnFailuresRule: TimberTestRule = TimberTestRule.logAllWhenTestFails()

    @Before
    fun setUp() {
        Timber.plant(Timber.DebugTree())

        every { residentIdProvider.getResidentId() } returns ID_NOT_REGISTERED
        every { residentIdProvider.hasProperResidentId() } returns false
        every { residentIdProvider.setResidentId(any()) } returns Unit
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

        every { residentApi.confirmDevice(ACTIVATION_CODE, FIREBASE_TOKEN, any(), any()) } answers {
            thirdArg<(Registration) -> Unit>().invoke(Registration(RESIDENT_ID))
        }
    }

    @Test
    fun onSuccessReturnsSuccess() = runBlockingTest {
        val result = sut.register()

        assertEquals(RegistrationResult.SUCCESS, result)
    }

    @Test
    fun shouldReturnIfAlreadyRegistered() = runBlockingTest {
        every { residentIdProvider.hasProperResidentId() } returns true

        val result = sut.register()

        assertEquals(RegistrationResult.ALREADY_REGISTERED, result)
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

        assertEquals(RegistrationResult.FAILURE, result)
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

        assertEquals(RegistrationResult.FAILURE, result)
    }

    @Test
    fun listensActivationCodeObserver() = runBlockingTest {
        sut.register()

        verify { activationCodeObserver.setListener(any()) }
    }

    @Test
    fun onActivationCodeTimeoutReturnsFailure() = runBlockingTest {
        every { activationCodeObserver.setListener(any()) } coAnswers {
            // NOTHING
        }

        val result = sut.register()
        advanceTimeBy(15_000)

        assertEquals(RegistrationResult.FAILURE, result)
    }

    @Test
    fun registersResident() = runBlockingTest {
        sut.register()

        verify { residentApi.confirmDevice(ACTIVATION_CODE, FIREBASE_TOKEN, any(), any()) }
    }

    @Test
    fun onResidentRegistrationFailureReturnsFailure() = runBlockingTest {
        every { residentApi.confirmDevice(ACTIVATION_CODE, FIREBASE_TOKEN, any(), any()) } answers {
            arg<(Exception) -> Unit>(3).invoke(IOException())
        }

        val result = sut.register()

        assertEquals(RegistrationResult.FAILURE, result)
    }

    @Test
    fun onSuccessSavesResidentId() = runBlockingTest {
        sut.register()

        verify { residentIdProvider.setResidentId(RESIDENT_ID) }
    }

    companion object {
        const val FIREBASE_TOKEN = "TOKEN"
        const val ACTIVATION_CODE = "ACTIVATION_CODE"
        const val RESIDENT_ID = "RESIDENT_ID"
    }
}
