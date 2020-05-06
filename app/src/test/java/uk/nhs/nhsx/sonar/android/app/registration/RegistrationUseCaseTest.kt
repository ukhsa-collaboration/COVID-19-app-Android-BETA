/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import androidx.work.workDataOf
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
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics
import uk.nhs.nhsx.sonar.android.app.analytics.registrationActivationCallFailed
import uk.nhs.nhsx.sonar.android.app.analytics.registrationFailedWaitingForActivationNotification
import uk.nhs.nhsx.sonar.android.app.analytics.registrationFailedWaitingForFCMToken
import uk.nhs.nhsx.sonar.android.app.analytics.registrationSendTokenCallFailed
import uk.nhs.nhsx.sonar.android.app.analytics.registrationSucceeded
import uk.nhs.nhsx.sonar.android.app.http.Promise.Deferred
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager.Companion.ACTIVATION_CODE_TIMED_OUT

@ExperimentalCoroutinesApi
class RegistrationUseCaseTest {

    private val tokenRetriever = mockk<TokenRetriever>()
    private val residentApi = mockk<ResidentApi>()
    private val activationCodeProvider = mockk<ActivationCodeProvider>(relaxUnitFun = true)
    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val postCodeProvider = mockk<PostCodeProvider>()
    private val analytics = mockk<SonarAnalytics>(relaxUnitFun = true)

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
            analytics,
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

        val result = registrationUseCase.register(workDataOf())

        assertThat(RegistrationResult.Success).isEqualTo(result)
    }

    @Test
    fun returnsSuccess() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE

        val result = registrationUseCase.register(workDataOf())

        verify { analytics.trackEvent(registrationSucceeded()) }
        assertThat(RegistrationResult.Success).isEqualTo(result)
    }

    @Test
    fun ifActivationCodeIsAbsent_RetrievesFirebaseToken() = runBlockingTest {
        registrationUseCase.register(workDataOf())

        coVerify { tokenRetriever.retrieveToken() }
    }

    @Test
    fun ifActivationCodeIsAbsent_RegistersDevice() = runBlockingTest {
        every { activationCodeProvider.get() } returns ""

        registrationUseCase.register(workDataOf())

        coVerify { residentApi.register(FIREBASE_TOKEN) }
    }

    @Test
    fun ifActivationCodeIsAbsent_ReturnsWaitingForActivationCode() = runBlockingTest {
        val result = registrationUseCase.register(workDataOf())

        assertThat(result).isEqualTo(RegistrationResult.WaitingForActivationCode)
    }

    @Test
    fun ifActivationCodeIsAbsent_AndActivationCodeTimedOutNotTrue() = runBlockingTest {
        registrationUseCase.register(workDataOf(ACTIVATION_CODE_TIMED_OUT to false))

        verify(exactly = 0) { analytics.trackEvent(registrationFailedWaitingForActivationNotification()) }
    }

    @Test
    fun ifActivationCodeIsAbsent_AndActivationCodeTimedOutIsTrue() = runBlockingTest {
        registrationUseCase.register(workDataOf(ACTIVATION_CODE_TIMED_OUT to true))

        verify { analytics.trackEvent(registrationFailedWaitingForActivationNotification()) }
    }

    @Test
    fun ifActivationCodeIsAbsentAndTokenRetrievalFailedReturnsFailure() = runBlockingTest {
        coEvery { tokenRetriever.retrieveToken() } throws RuntimeException("Firebase is not available")

        val result = registrationUseCase.register(workDataOf())

        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun onDeviceRegistrationFailure() = runBlockingTest {
        registerDeviceFails(statusCode = 403)

        val result = registrationUseCase.register(workDataOf())

        verify { analytics.trackEvent(registrationSendTokenCallFailed(403)) }
        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun ifActivationCodeIsPresentAndTokenRetrievalFailed() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE
        coEvery { tokenRetriever.retrieveToken() } throws RuntimeException("Firebase is not available")

        val result = registrationUseCase.register(workDataOf())

        verify { analytics.trackEvent(registrationFailedWaitingForFCMToken()) }
        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun ifActivationCodeIsPresentRegistersResident() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE
        registrationUseCase.register(workDataOf())

        verify { residentApi.confirmDevice(confirmation) }
    }

    @Test
    fun onResidentRegistrationClientErrorClearsActivationCode() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE
        confirmDeviceFails(ClientError())

        registrationUseCase.register(workDataOf())

        verify { activationCodeProvider.clear() }
    }

    @Test
    fun onResidentRegistrationClientErrorReturnsError() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE
        confirmDeviceFails(ClientError(buildNetworkResponse(statusCode = 400)))

        val result = registrationUseCase.register(workDataOf())

        verify { analytics.trackEvent(registrationActivationCallFailed(400)) }
        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun onResidentRegistrationAllOtherErrors() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE
        confirmDeviceFails(VolleyError(buildNetworkResponse(statusCode = 503)))

        val result = registrationUseCase.register(workDataOf())

        verify(exactly = 0) {
            activationCodeProvider.clear()
        }
        verify { analytics.trackEvent(registrationActivationCallFailed(503)) }
        assertThat(result).isEqualTo(RegistrationResult.Error)
    }

    @Test
    fun onSuccessSavesSonarId() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE

        registrationUseCase.register(workDataOf())

        verify { sonarIdProvider.set(RESIDENT_ID) }
    }

    @Test
    fun onSuccessSavesSendsSuccessfulRegistrationAnalyticEvent() = runBlockingTest {
        every { activationCodeProvider.get() } returns ACTIVATION_CODE

        registrationUseCase.register(workDataOf())

        verify { analytics.trackEvent(registrationSucceeded()) }
    }

    private fun confirmDeviceFails(exception: Exception) {
        val deferred = Deferred<Registration>()
        deferred.fail(exception)
        every { residentApi.confirmDevice(confirmation) } returns deferred.promise
    }

    private fun registerDeviceFails(statusCode: Int) {
        val networkResponse = buildNetworkResponse(statusCode)
        val deferred = Deferred<Unit>()

        deferred.fail(VolleyError(networkResponse))
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
