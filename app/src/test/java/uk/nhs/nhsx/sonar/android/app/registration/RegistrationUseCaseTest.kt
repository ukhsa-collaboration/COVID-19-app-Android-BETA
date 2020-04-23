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
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import uk.nhs.nhsx.sonar.android.client.DeviceConfirmation
import uk.nhs.nhsx.sonar.android.client.Registration
import uk.nhs.nhsx.sonar.android.client.ResidentApi
import uk.nhs.nhsx.sonar.android.client.http.Promise.Deferred
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
    fun setUp() {
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
    fun returnsSuccess() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE

        val result = registrationUseCase.register()

        assertThat(RegistrationResult.Success).isEqualTo(result)
    }

    @Test
    fun returnsAlreadyRegistered() = runBlockingTest {
        every { sonarIdProvider.hasProperSonarId() } returns true

        val result = registrationUseCase.register()

        assertThat(RegistrationResult.AlreadyRegistered).isEqualTo(result)
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
        every { activationCodeProvider.getActivationCode() } returns ""

        val result = registrationUseCase.register()

        assertThat(RegistrationResult.WaitingForActivationCode).isEqualTo(result)
    }

    @Test
    fun ifActivationCodeIsAbsentAndTokenRetrievalFailedReturnsFailure() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ""
        coEvery { tokenRetriever.retrieveToken() } throws RuntimeException("Firebase is not available")

        val result = registrationUseCase.register()

        assertThat(result).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test
    fun onDeviceRegistrationFailureReturnsFailure() = runBlockingTest {
        residentApiCallFails()

        val result = registrationUseCase.register()

        assertThat(result).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test
    fun ifActivationCodeIsPresentAndTokenRetrievalFailedReturnsFailure() = runBlockingTest {
        coEvery { tokenRetriever.retrieveToken() } throws RuntimeException("Firebase is not available")

        val result = registrationUseCase.register()

        assertThat(result).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test
    fun registersResident() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE
        registrationUseCase.register()

        verify { residentApi.confirmDevice(confirmation) }
    }

    @Test
    fun onResidentRegistrationClientErrorReturnsActivationCodeNotValidFailure() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE
        confirmDeviceApiCallFails(ClientError())

        val result = registrationUseCase.register()

        verify { activationCodeProvider.clear() }
        assertThat(result).isInstanceOf(RegistrationResult.ActivationCodeNotValidFailure::class.java)
    }

    @Test
    fun onResidentRegistrationOtherErrorReturnsFailure() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE
        confirmDeviceApiCallFails(IOException())

        val result = registrationUseCase.register()

        verify(exactly = 0) {
            activationCodeProvider.clear()
        }
        assertThat(result).isInstanceOf(RegistrationResult.Failure::class.java)
    }

    @Test
    fun onSuccessSavesSonarId() = runBlockingTest {
        every { activationCodeProvider.getActivationCode() } returns ACTIVATION_CODE

        registrationUseCase.register()

        verify { sonarIdProvider.setSonarId(RESIDENT_ID) }
    }

    private fun confirmDeviceApiCallFails(exception: Exception) {
        val deferred = Deferred<Registration>()
        deferred.fail(exception)
        every { residentApi.confirmDevice(confirmation) } returns deferred.promise
    }

    private fun residentApiCallFails() {
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
