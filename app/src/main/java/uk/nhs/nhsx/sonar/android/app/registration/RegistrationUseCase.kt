/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import androidx.work.Data
import com.android.volley.ClientError
import com.android.volley.VolleyError
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics
import uk.nhs.nhsx.sonar.android.app.analytics.registrationActivationCallFailed
import uk.nhs.nhsx.sonar.android.app.analytics.registrationFailedWaitingForActivationNotification
import uk.nhs.nhsx.sonar.android.app.analytics.registrationFailedWaitingForFCMToken
import uk.nhs.nhsx.sonar.android.app.analytics.registrationSendTokenCallFailed
import uk.nhs.nhsx.sonar.android.app.analytics.registrationSucceeded
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager.Companion.ACTIVATION_CODE_TIMED_OUT
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RegistrationUseCase @Inject constructor(
    private val tokenRetriever: TokenRetriever,
    private val residentApi: ResidentApi,
    private val sonarIdProvider: SonarIdProvider,
    private val postCodeProvider: PostCodeProvider,
    private val activationCodeProvider: ActivationCodeProvider,
    private val analytics: SonarAnalytics,
    @Named(AppModule.DEVICE_MODEL) private val deviceModel: String,
    @Named(AppModule.DEVICE_OS_VERSION) private val deviceOsVersion: String
) {

    suspend fun register(inputData: Data): RegistrationResult {
        try {
            if (sonarIdProvider.hasProperSonarId()) {
                Timber.d("Already registered")
                return RegistrationResult.Success
            }
            val activationCode = activationCodeProvider.getActivationCode()
            if (activationCode.isEmpty()) {
                if (inputData.getBoolean(ACTIVATION_CODE_TIMED_OUT, false)) {
                    analytics.trackEvent(registrationFailedWaitingForActivationNotification())
                }

                val firebaseToken = getFirebaseToken()
                Timber.d("firebaseToken = $firebaseToken")
                registerDevice(firebaseToken)
                Timber.d("registered device")
                return RegistrationResult.WaitingForActivationCode
            }
            val firebaseToken = getFirebaseToken()
            val sonarId =
                registerResident(activationCode, firebaseToken, postCodeProvider.getPostCode())
            Timber.d("sonarId = $sonarId")
            storeSonarId(sonarId)
            Timber.d("sonarId stored")
            analytics.trackEvent(registrationSucceeded())
            return RegistrationResult.Success
        } catch (e: ClientError) {
            // TODO: delete firebase token?
            activationCodeProvider.clear()
            return RegistrationResult.Error
        } catch (e: Exception) {
            Timber.e(e, "RegistrationUseCase exception")
            return RegistrationResult.Error
        }
    }

    private suspend fun getFirebaseToken(): Token =
        try {
            tokenRetriever.retrieveToken()
        } catch (e: Exception) {
            analytics.trackEvent(registrationFailedWaitingForFCMToken())
            throw e
        }

    private suspend fun registerDevice(firebaseToken: String) =
        residentApi
            .register(firebaseToken)
            .onError { analytics.trackEvent(registrationSendTokenCallFailed(it.getStatusCode())) }
            .toCoroutine()

    private suspend fun registerResident(
        activationCode: String,
        firebaseToken: String,
        postCode: String
    ): String {
        val confirmation = DeviceConfirmation(
            activationCode = activationCode,
            pushToken = firebaseToken,
            deviceModel = deviceModel,
            deviceOsVersion = deviceOsVersion,
            postalCode = postCode
        )

        return residentApi
            .confirmDevice(confirmation)
            .onError { analytics.trackEvent(registrationActivationCallFailed(it.getStatusCode())) }
            .map { it.id }
            .toCoroutine()
    }

    private fun storeSonarId(sonarId: String) {
        sonarIdProvider.setSonarId(sonarId)
    }

    private fun Exception.getStatusCode(): Int? =
        when (this) {
            is VolleyError -> networkResponse?.statusCode
            else -> null
        }
}
