/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import com.android.volley.ClientError
import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
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
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise.Deferred
import uk.nhs.nhsx.sonar.android.app.http.failWithVolleyError
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider

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

    @get:Rule
    val logAllOnFailuresRule: TimberTestRule = TimberTestRule.logAllWhenTestFails()

    @Before
    fun setupFirstEntry() {
        Timber.plant(Timber.DebugTree())

        every { sonarIdProvider.get() } returns ""
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { sonarIdProvider.set(any()) } returns Unit
        coEvery { tokenRetriever.retrieveToken() } returns FIREBASE_TOKEN

        val registrationDeferred = Deferred<Unit>()
        registrationDeferred.resolve(Unit)
        every { residentApi.register(FIREBASE_TOKEN) } returns registrationDeferred.promise

        every { activationCodeProvider.get() } returns ""

        val confirmationDeferred = Deferred<Registration>()
        confirmationDeferred.resolve(Registration(RESIDENT_ID))
        every { residentApi.confirmDevice(confirmation) } returns confirmationDeferred.promise

        every { postCodeProvider.get() } returns POST_CODE
    }

    @Test
    fun ifAlreadyRegisteredReturnsSuccess() = runBlockingTest {
        every { sonarIdProvider.hasProperSonarId() } returns true

        val result = registrationUseCase.register()

        assertThat(RegistrationResult.Success).isEqualTo(result)
    }

    @Test
    fun returnsSuccess() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE

        val result = registrationUseCase.register()

        assertThat(RegistrationResult.Success).isEqualTo(result)
    }

    @Test
    fun ifActivationCodeIsAbsent_RetrievesFirebaseToken() = runBlockingTest {
        registrationUseCase.register()

        coVerify { tokenRetriever.retrieveToken() }
    }

    @Test
    fun ifActivationCodeIsAbsent_RegistersDevice() = runBlockingTest {
        every { activationCodeProvider.get() } returns ""

        registrationUseCase.register()

        coVerify { residentApi.register(FIREBASE_TOKEN) }
    }

    @Test
    fun ifActivationCodeIsAbsent_ReturnsWaitingForActivationCode() = runBlockingTest {
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
    fun onDeviceRegistrationFailure() = runBlockingTest {
        registerDeviceFails(statusCode = 403)

        val result = registrationUseCase.register()

        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun ifActivationCodeIsPresentAndTokenRetrievalFailed() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE
        coEvery { tokenRetriever.retrieveToken() } throws RuntimeException("Firebase is not available")

        val result = registrationUseCase.register()

        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun ifActivationCodeIsPresentRegistersResident() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE
        registrationUseCase.register()

        verify { residentApi.confirmDevice(confirmation) }
    }

    @Test
    fun onResidentRegistrationClientErrorClearsActivationCode() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE
        confirmDeviceFails(ClientError())

        registrationUseCase.register()

        verify { activationCodeProvider.clear() }
    }

    @Test
    fun onResidentRegistrationClientErrorReturnsError() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE
        confirmDeviceFails(ClientError(buildNetworkResponse(statusCode = 400)))

        val result = registrationUseCase.register()

        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun onResidentRegistrationAllOtherErrors() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE
        confirmDeviceFails(VolleyError(buildNetworkResponse(statusCode = 503)))

        val result = registrationUseCase.register()

        verify(exactly = 0) {
            activationCodeProvider.clear()
        }
        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun onSuccessSavesSonarId() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE

        registrationUseCase.register()

        verify { sonarIdProvider.set(RESIDENT_ID) }
    }

    private fun confirmDeviceFails(error: VolleyError) {
        val deferred = Deferred<Registration>()
        deferred.failWithVolleyError(error)
        every { residentApi.confirmDevice(confirmation) } returns deferred.promise
    }

    private fun registerDeviceFails(statusCode: Int) {
        val networkResponse = buildNetworkResponse(statusCode)
        val deferred = Deferred<Unit>()

        deferred.failWithVolleyError(VolleyError(networkResponse))
        every { residentApi.register(any()) } returns deferred.promise
    }

    private fun buildNetworkResponse(statusCode: Int) =
        NetworkResponse(statusCode, ByteArray(0), true, 0L, listOf())

    companion object {
        const val FIREBASE_TOKEN = "::firebase token::"
        const val ACTIVATION_CODE = "::activation code::"
        const val DEVICE_MODEL = "::device model::"
        const val DEVICE_OS_VERSION = "24"
        const val RESIDENT_ID = "::resident id::"
        const val POST_CODE = "::postal code::"
    }
}
