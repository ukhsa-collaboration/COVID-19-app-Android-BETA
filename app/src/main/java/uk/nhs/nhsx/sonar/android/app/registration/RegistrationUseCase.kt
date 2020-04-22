/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import com.android.volley.ClientError
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import uk.nhs.nhsx.sonar.android.client.DeviceConfirmation
import uk.nhs.nhsx.sonar.android.client.ResidentApi
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine

typealias Token = String

@Singleton
class RegistrationUseCase @Inject constructor(
    private val tokenRetriever: TokenRetriever,
    private val residentApi: ResidentApi,
    private val sonarIdProvider: SonarIdProvider,
    private val postCodeProvider: PostCodeProvider,
    private val activationCodeProvider: ActivationCodeProvider,
    @Named(AppModule.DEVICE_MODEL) private val deviceModel: String,
    @Named(AppModule.DEVICE_OS_VERSION) private val deviceOsVersion: String
) {

    suspend fun register(): RegistrationResult {
        try {
            if (sonarIdProvider.hasProperSonarId()) {
                Timber.d("Already registered")
                return RegistrationResult.AlreadyRegistered
            }
            val activationCode = activationCodeProvider.getActivationCode()
            if (activationCode.isEmpty()) {
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
            return RegistrationResult.Success
        } catch (e: ClientError) {
            activationCodeProvider.clear()
            return RegistrationResult.ActivationCodeNotValidFailure(e)
        } catch (e: Exception) {
            Timber.e(e, "RegistrationUseCase exception")
            return RegistrationResult.Failure(e)
        }
    }

    private suspend fun getFirebaseToken(): Token {
        when (val result = tokenRetriever.retrieveToken()) {
            is TokenRetriever.Result.Success -> {
                return result.token
            }
            is TokenRetriever.Result.Failure -> {
                if (result.exception != null) {
                    throw result.exception
                } else {
                    throw RuntimeException("Cannot get Firebase token")
                }
            }
        }
    }

    private suspend fun registerDevice(firebaseToken: String) {
        return suspendCoroutine { continuation ->
            residentApi
                .register(firebaseToken)
                .onSuccess { continuation.resumeWith(Result.success(Unit)) }
                .onError { continuation.resumeWith(Result.failure(it)) }
        }
    }

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

        return suspendCoroutine { continuation ->
            residentApi
                .confirmDevice(confirmation)
                .onSuccess { continuation.resumeWith(Result.success(it.id)) }
                .onError { continuation.resumeWith(Result.failure(it)) }
        }
    }

    private fun storeSonarId(sonarId: String) {
        sonarIdProvider.setSonarId(sonarId)
    }
}
