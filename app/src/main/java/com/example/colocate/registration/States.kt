package com.example.colocate.registration

import java.lang.Exception

sealed class RegistrationResult {
    object Initial : RegistrationResult()
    data class FCMTokenReceived(val fcmid: String) : RegistrationResult()
    object RegisteredDevice : RegistrationResult()
    data class FirebaseMessageReceived(val activationCode: String) : RegistrationResult()
    data class CitizenRegistered(val residentId: String) : RegistrationResult()
    object Finished : RegistrationResult()
    data class Retry(val exception: Exception?) : RegistrationResult()
}

sealed class RegistrationActions {
    object GetFirebaseToken : RegistrationActions()
    data class RegisterDevice(val fbmId: String) : RegistrationActions()
    object GetFirebaseActivationCode : RegistrationActions()
    data class RegisterCitizen(val activationCode: String) : RegistrationActions()
    data class SaveCitizenId(val citizenId: String) : RegistrationActions()
    object Finish : RegistrationActions()
}