/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpMethod
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import javax.inject.Inject

class AcknowledgmentsApi @Inject constructor(private val httpClient: HttpClient) {

    fun send(url: String) {
        httpClient.send(HttpRequest(HttpMethod.PUT, url, null))
    }
}
