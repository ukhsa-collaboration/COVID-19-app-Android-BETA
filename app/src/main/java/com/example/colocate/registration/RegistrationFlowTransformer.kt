package com.example.colocate.registration

import com.example.colocate.persistence.FCMPushTokenProvider
import com.example.colocate.persistence.ResidentIdProvider
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi
import java.lang.IllegalStateException
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class RegistrationFlowTransformer @Inject constructor(
    private val residentApi: ResidentApi,
    private val firebaseTokenRetriever: TokenRetriever,
    private val residentIdProvider: ResidentIdProvider,
    private val fcmPushTokenProvider: FCMPushTokenProvider
) {

    suspend fun process(actions: RegistrationActions): RegistrationResult {
        return when (actions) {
            RegistrationActions.GetFirebaseToken -> getFirebaseToken()
            is RegistrationActions.RegisterDevice -> registerDevice(actions.fbmId)
            RegistrationActions.GetFirebaseActivationCode -> handleFirebaseActivation()
            is RegistrationActions.RegisterCitizen -> registerCitizen(actions.activationCode)
            is RegistrationActions.SaveCitizenId -> storeCitizen(actions.citizenId)
            RegistrationActions.Finish -> RegistrationResult.Finished
        }
    }

    private fun handleFirebaseActivation(): RegistrationResult {
        throw IllegalStateException("Invalid state, this state should be triggered from the notification service")
    }

    private fun storeCitizen(citizenId: String): RegistrationResult {
        residentIdProvider.setResidentId(citizenId)
        return RegistrationResult.Finished
    }

    private suspend fun registerCitizen(activationCode: String): RegistrationResult {
        return suspendCoroutine { continuation ->
            residentApi.confirmDevice(
                activationCode,
                fcmPushTokenProvider.getToken(),
                onSuccess = {
                    continuation.resumeWith(
                        Result.success(
                            RegistrationResult.CitizenRegistered(
                                it.id
                            )
                        )
                    )
                },
                onError = {
                    continuation.resumeWith(Result.success(RegistrationResult.Retry(it)))
                }
            )
        }
    }

    private suspend fun registerDevice(fbmId: String): RegistrationResult {
        return suspendCoroutine { continuation ->
            residentApi.register(fbmId,
                onSuccess = { continuation.resumeWith(Result.success(RegistrationResult.RegisteredDevice)) },
                onError = {
                    continuation.resumeWith(Result.success(RegistrationResult.Retry(it)))
                }
            )
        }
    }

    private suspend fun getFirebaseToken(): RegistrationResult {
        return when (val tokenStatus = firebaseTokenRetriever.retrieveToken()) {
            is TokenRetriever.Result.Success -> {
                fcmPushTokenProvider.setToken(tokenStatus.token)
                RegistrationResult.FCMTokenReceived(fcmid = tokenStatus.token)
            }
            is TokenRetriever.Result.Failure -> RegistrationResult.Retry(tokenStatus.exception)
        }
    }
}