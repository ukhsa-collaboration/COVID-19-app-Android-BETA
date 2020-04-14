package uk.nhs.nhsx.sonar.android.app.registration

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.persistence.PostCodeProvider
import uk.nhs.nhsx.sonar.android.app.persistence.SonarIdProvider
import uk.nhs.nhsx.sonar.android.client.resident.DeviceConfirmation
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias Token = String

@Singleton
class RegistrationUseCase @Inject constructor(
    private val tokenRetriever: TokenRetriever,
    private val residentApi: ResidentApi,
    private val activationCodeObserver: ActivationCodeObserver,
    private val sonarIdProvider: SonarIdProvider,
    private val postCodeProvider: PostCodeProvider,
    @Named(AppModule.DEVICE_MODEL) private val deviceModel: String,
    @Named(AppModule.DEVICE_OS_VERSION) private val deviceOsVersion: String
) {

    suspend fun register(): RegistrationResult {
        try {
            return withTimeout(20_000) {
                if (sonarIdProvider.hasProperSonarId()) {
                    Timber.d("Already registered")
                    return@withTimeout RegistrationResult.AlreadyRegistered
                }
                val firebaseToken = getFirebaseToken()
                Timber.d("RegistrationUseCase firebaseToken = $firebaseToken")
                registerDevice(firebaseToken)
                Timber.d("RegistrationUseCase registered device")
                val activationCode = waitForActivationCode()
                Timber.d("RegistrationUseCase activationCode = $activationCode")
                val sonarId =
                    registerResident(activationCode, firebaseToken, postCodeProvider.getPostCode())
                Timber.d("RegistrationUseCase sonarId = $sonarId")
                storeSonarId(sonarId)
                Timber.d("RegistrationUseCase sonarId stored")
                return@withTimeout RegistrationResult.Success
            }
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
            residentApi.register(firebaseToken,
                onSuccess = { continuation.resumeWith(Result.success(Unit)) },
                onError = { continuation.resumeWith(Result.failure(it)) }
            )
        }
    }

    private suspend fun waitForActivationCode(): String {
        return suspendCancellableCoroutine { continuation ->
            activationCodeObserver.setListener { activationCode ->
                activationCodeObserver.removeListener()
                continuation.resume(activationCode)
            }
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
            residentApi.confirmDevice(
                confirmation,
                onSuccess = { continuation.resumeWith(Result.success(it.id)) },
                onError = { continuation.resumeWith(Result.failure(it)) }
            )
        }
    }

    private fun storeSonarId(sonarId: String) {
        sonarIdProvider.setSonarId(sonarId)
    }
}
