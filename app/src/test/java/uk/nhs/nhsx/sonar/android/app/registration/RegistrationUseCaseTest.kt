/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import com.android.volley.ClientError
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
import uk.nhs.nhsx.sonar.android.app.http.Promise.Deferred
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import java.io.IOException

@ExperimentalCoroutinesApi
class RegistrationUseCaseTest {

    private val tokenRetriever = mockk<TokenRetriever>()
    private val residentApi = mockk<ResidentApi>()
    private val activationCodeProvider = mockk<ActivationCodeProvider>(relaxUnitFun = true)
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
    fun setupFirstEntry() {
        Timber.plant(Timber.DebugTree())

        every { sonarIdProvider.getSonarId() } returns ""
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { sonarIdProvider.setSonarId(any()) } returns Unit
        coEvery { tokenRetriever.retrieveToken() } returns FIREBASE_TOKEN

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
    fun ifAlreadyRegisteredReturnsSuccess() = runBlockingTest {
        every { sonarIdProvider.hasProperSonarId() } returns true

        val result = registrationUseCase.register()

        assertThat(RegistrationResult.Success).isEqualTo(result)
    }

    @Test
    fun returnsSuccess() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE

        val result = registrationUseCase.register()

        assertThat(RegistrationResult.Success).isEqualTo(result)
    }

    @Test
    fun ifActivationCodeIsAbsentRetrievesFirebaseToken() = runBlockingTest {
        registrationUseCase.register()

        coVerify { tokenRetriever.retrieveToken() }
    }

    @Test
    fun ifActivationCodeIsAbsentRegistersDevice() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ""

        registrationUseCase.register()

        coVerify { residentApi.register(FIREBASE_TOKEN) }
    }

    @Test
    fun ifActivationCodeIsAbsentReturnsWaitingForActivationCode() = runBlockingTest {
        val result = registrationUseCase.register()

        assertThat(result).isEqualTo(RegistrationResult.WaitingForActivationCode)
    }

    @Test
    fun ifActivationCodeIsAbsentAndTokenRetrievalFailedReturnsFailure() = runBlockingTest {
        coEvery { tokenRetriever.retrieveToken() } throws RuntimeException("Firebase is not available")

        val result = registrationUseCase.register()

        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun onDeviceRegistrationFailureReturnsFailure() = runBlockingTest {
        registerDeviceFails()

        val result = registrationUseCase.register()

        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun ifActivationCodeIsPresentAndTokenRetrievalFailedReturnsFailure() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE
        coEvery { tokenRetriever.retrieveToken() } throws RuntimeException("Firebase is not available")

        val result = registrationUseCase.register()

        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun ifActivationCodeIsPresentRegistersResident() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE
        registrationUseCase.register()

        verify { residentApi.confirmDevice(confirmation) }
    }

    @Test
    fun onResidentRegistrationClientErrorClearsActivationCode() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE
        confirmDeviceFails(ClientError())

        registrationUseCase.register()

        verify { activationCodeProvider.clear() }
    }

    @Test
    fun onResidentRegistrationClientErrorReturnsError() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE
        confirmDeviceFails(ClientError())

        val result = registrationUseCase.register()

        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun onResidentRegistrationAllOtherErrorsReturnsFailure() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE
        confirmDeviceFails(IOException())

        val result = registrationUseCase.register()

        verify(exactly = 0) {
            activationCodeProvider.clear()
        }
        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun onSuccessSavesSonarId() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE

        registrationUseCase.register()

        verify { sonarIdProvider.setSonarId(RESIDENT_ID) }
    }

    private fun confirmDeviceFails(exception: Exception) {
        val deferred = Deferred<Registration>()
        deferred.fail(exception)
        every { residentApi.confirmDevice(confirmation) } returns deferred.promise
    }

    private fun registerDeviceFails() {
        val deferred = Deferred<Unit>()
        deferred.fail(IOException())
        every { residentApi.register(any()) } returns deferred.promise
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
