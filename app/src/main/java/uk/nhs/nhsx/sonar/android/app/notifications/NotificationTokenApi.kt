package uk.nhs.nhsx.sonar.android.app.notifications

import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.HttpMethod.PUT
import uk.nhs.nhsx.sonar.android.app.http.HttpRequest
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf

class NotificationTokenApi(
    private val baseUrl: String,
    private val secretKeyStorage: SecretKeyStorage,
    private val httpClient: HttpClient
) {

    fun updateToken(sonarId: String, newToken: String): Promise<Unit> {
        val secretKey = secretKeyStorage.provideSecretKey()
        val jsonBody = jsonObjectOf(
            "sonarId" to sonarId,
            "pushNotificationToken" to newToken
        )

        val request = HttpRequest(PUT, "$baseUrl/api/registration/push-notification-token", jsonBody, secretKey)

        return httpClient.send(request).mapToUnit()
    }
}
