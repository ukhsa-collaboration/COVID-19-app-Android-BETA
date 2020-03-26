package com.example.colocate.registration

import com.example.colocate.di.module.AppModule
import com.example.colocate.registration.RegistrationActions.*
import com.example.colocate.registration.RegistrationResult.*
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RegistrationFlowStore @Inject constructor(
    private val registrationFlowTransformer: RegistrationFlowTransformer,
    @Named(AppModule.DISPATCHER_IO) private val dispatcher: CoroutineDispatcher
) {

    private var currentAction: RegistrationActions = GetFirebaseToken

    private val coroutineScope = CoroutineScope(dispatcher + Job())


    fun start() {
        coroutineScope.launch {
            while (currentAction != GetFirebaseActivationCode && currentAction != Finish) {
                Timber.d("Action is %s", currentAction.toString())
                currentAction =
                    registrationFlowTransformer
                        .process(currentAction).let {
                            reduce(it)
                        }
            }
        }

    }

    fun setActivationCode(activationCode: String) {
        Timber.d("Action is code $activationCode")
        Timber.d("Action is $currentAction")
        if (currentAction == GetFirebaseActivationCode) {
            currentAction = RegisterCitizen(activationCode)
            start()
        }

    }


    private fun reduce(result: RegistrationResult): RegistrationActions {
        return when (result) {
            Initial -> GetFirebaseToken
            is FCMTokenReceived -> RegisterDevice(result.fcmid)
            RegisteredDevice -> GetFirebaseActivationCode
            is FirebaseMessageReceived -> RegisterCitizen(result.activationCode)
            is CitizenRegistered -> SaveCitizenId(result.residentId)
            Finished -> Finish
            is Retry -> TODO()
        }
    }
}