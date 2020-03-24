package uk.nhs.nhsx.sonar.android.client.resident

import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import java.lang.Exception

class ResidentApi(private val httpClient: HttpClient) {

    fun register(onSuccess: (Registration) -> Unit = {}, onError: (Exception) -> Unit = {}) {
        httpClient.post(HttpRequest("/api/residents", JSONObject()), {
            json: JSONObject -> onSuccess(mapResponseToRegistration(json))
        }, {
            error: Exception -> onError(error)
        })
    }

    private fun mapResponseToRegistration(jsonObject: JSONObject): Registration {
        val residentId = jsonObject.getString("id")
        val hmacKey = jsonObject.getString("hmacKey")

        return Registration(
            residentId,
            hmacKey
        )
    }
}
