package uk.nhs.nhsx.sonar.android.client

import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import javax.inject.Inject

class AcknowledgementsApi @Inject constructor(private val httpClient: HttpClient) {

    fun send(url: String) {
        val acknowledgementRequest = HttpRequest(url, null, null)
        httpClient.post(acknowledgementRequest, {}, {})
    }
}
