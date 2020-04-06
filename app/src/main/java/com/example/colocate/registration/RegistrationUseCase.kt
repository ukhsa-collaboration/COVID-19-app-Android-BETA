package com.example.colocate.registration

import com.example.colocate.di.module.AppModule
import com.example.colocate.persistence.ResidentIdProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.client.resident.DeviceConfirmation
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias Token = String

@Singleton
class RegistrationUseCase @Inject constructor(
    private val tokenRetriever: TokenRetriever,
    private val residentApi: ResidentApi,
    private val activationCodeObserver: ActivationCodeObserver,
    private val residentIdProvider: ResidentIdProvider,
    @Named(AppModule.DEVICE_MODEL) private val deviceModel: String,
    @Named(AppModule.DEVICE_OS_VERSION) private val deviceOsVersion: String
) {

    suspend fun register(): RegistrationResult {
        try {
            if (residentIdProvider.hasProperResidentId()) {
                Timber.d("Already registered")
                return RegistrationResult.AlreadyRegistered
            }
            val firebaseToken = getFirebaseToken()
            Timber.d("RegistrationUseCase firebaseToken = $firebaseToken")
            registerDevice(firebaseToken)
            Timber.d("RegistrationUseCase registered device")
            val activationCode = waitForActivationCode(10_000)
            Timber.d("RegistrationUseCase activationCode = $activationCode")
            val residentId = registerResident(activationCode, firebaseToken)
            Timber.d("RegistrationUseCase residentId = $residentId")
            storeResidentId(residentId)
            Timber.d("RegistrationUseCase residentId stored")
            return RegistrationResult.Success
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

    private suspend fun waitForActivationCode(timeout: Long): String {
        return suspendCoroutineWithTimeout(timeout) { continuation ->
            activationCodeObserver.setListener { activationCode ->
                activationCodeObserver.removeListener()
                continuation.resume(activationCode)
            }
        }
    }

    private suspend fun registerResident(activationCode: String, firebaseToken: String): String {
        val confirmation = DeviceConfirmation(
            activationCode = activationCode,
            pushToken = firebaseToken,
            deviceModel = deviceModel,
            deviceOsVersion = deviceOsVersion
        )

        return suspendCoroutine { continuation ->
            residentApi.confirmDevice(
                confirmation,
                onSuccess = { continuation.resumeWith(Result.success(it.id)) },
                onError = { continuation.resumeWith(Result.failure(it)) }
            )
        }
    }

    private fun storeResidentId(citizenId: String) {
        residentIdProvider.setResidentId(citizenId)
    }

    private suspend inline fun <T> suspendCoroutineWithTimeout(
        timeout: Long,
        crossinline block: (Continuation<T>) -> Unit
    ) = withTimeout(timeout) {
        suspendCancellableCoroutine(block = block)
    }
}
