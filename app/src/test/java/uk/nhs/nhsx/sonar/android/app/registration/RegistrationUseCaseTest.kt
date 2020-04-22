/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import net.lachlanmckee.timberjunit.TimberTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.TokenRetriever.Result.Success
import uk.nhs.nhsx.sonar.android.client.DeviceConfirmation
import uk.nhs.nhsx.sonar.android.client.Registration
import uk.nhs.nhsx.sonar.android.client.ResidentApi
import uk.nhs.nhsx.sonar.android.client.http.Promise.Deferred
import java.io.IOException

@ExperimentalCoroutinesApi
class RegistrationUseCaseTest {

    private val tokenRetriever = mockk<TokenRetriever>()
    private val residentApi = mockk<ResidentApi>()
    private val activationCodeProvider = mockk<ActivationCodeProvider>()
    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val postCodeProvider = mockk<PostCodeProvider>()

    private val confirmation =
        DeviceConfirmation(
            ACTIVATION_CODE,
            FIREBASE_TOKEN,
            DEVICE_MODEL,
            DEVICE_OS_VERSION,
            POST_CODE
        )

    private val registrationUseCase =
        RegistrationUseCase(
            tokenRetriever,
            residentApi,
            sonarIdProvider,
            postCodeProvider,
            activationCodeProvider,
            DEVICE_MODEL,
            DEVICE_OS_VERSION
        )

    @Rule
    @JvmField
    val logAllOnFailuresRule: TimberTestRule = TimberTestRule.logAllWhenTestFails()

    @Before
    fun setUp() {
        Timber.plant(Timber.DebugTree())

        every { sonarIdProvider.getSonarId() } returns ""
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { sonarIdProvider.setSonarId(any()) } returns Unit
        coEvery { tokenRetriever.retrieveToken() } returns Success(FIREBASE_TOKEN)
        val registrationDeferred = Deferred<Unit>()
        registrationDeferred.resolve(Unit)
        every { residentApi.register(FIREBASE_TOKEN) } returns registrationDeferred.promise

        every { activationCodeProvider.getActivationCode() } returns ""

        val confirmationDeferred = Deferred<Registration>()
        confirmationDeferred.resolve(Registration(RESIDENT_ID))
        every { residentApi.confirmDevice(confirmation) } returns confirmationDeferred.promise

        every { postCodeProvider.getPostCode() } returns POST_CODE
    }

    @Test
    fun onSuccessReturnsSuccess() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE

        val result = registrationUseCase.register()

        assertThat(RegistrationResult.Success).isEqualTo(result)
    }

    @Test
    fun shouldReturnIfAlreadyRegistered() = runBlockingTest {
        every { sonarIdProvider.hasProperSonarId() } returns true

        val result = registrationUseCase.register()

        assertThat(RegistrationResult.AlreadyRegistered).isEqualTo(result)
    }

    @Test
    fun retrievesFirebaseToken() = runBlockingTest {
        registrationUseCase.register()

        coVerify { tokenRetriever.retrieveToken() }
    }

    @Test
    fun onTokenRetrievalFailureReturnFailure() = runBlockingTest {
        coEvery { tokenRetriever.retrieveToken() } returns TokenRetriever.Result.Failure(null)

        val result = registrationUseCase.register()

        assertThat(result).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test
    fun registersDevice() = runBlockingTest {
        registrationUseCase.register()

        verify { residentApi.register(FIREBASE_TOKEN) }
    }

    @Test
    fun onDeviceRegistrationFailureReturnsFailure() = runBlockingTest {
        val deferred = Deferred<Unit>()
        deferred.fail(IOException())
        every { residentApi.register(any()) } returns deferred.promise

        val result = registrationUseCase.register()

        assertThat(result).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test
    fun callsActivationCodeObserver() = runBlockingTest {
        registrationUseCase.register()

        verify { activationCodeProvider.getActivationCode() }
    }

    @Test
    fun registersResident() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE

        registrationUseCase.register()

        verify { residentApi.confirmDevice(confirmation) }
    }

    @Test
    fun onResidentRegistrationFailureReturnsFailure() = runBlockingTest {
        val deferred = Deferred<Registration>()
        deferred.fail(IOException())
        every { residentApi.confirmDevice(confirmation) } returns deferred.promise
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE

        val result = registrationUseCase.register()

        verify { residentApi.confirmDevice(confirmation) }
        assertThat(result).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test
    fun onSuccessSavesSonarId() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE

        registrationUseCase.register()

        verify { sonarIdProvider.setSonarId(RESIDENT_ID) }
    }

    companion object {
        const val FIREBASE_TOKEN = "::firebase token::"
        const val ACTIVATION_CODE = "::activation code::"
        const val DEVICE_MODEL = "::device model::"
        const val DEVICE_OS_VERSION = "24"
        const val RESIDENT_ID = "::resident id::"
        const val POST_CODE = "::postal code::"
    }
}
