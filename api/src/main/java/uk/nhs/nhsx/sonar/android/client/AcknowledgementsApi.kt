/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client

import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpMethod.POST
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import javax.inject.Inject

class AcknowledgementsApi @Inject constructor(private val httpClient: HttpClient) {

    fun send(url: String) {
        httpClient.send(HttpRequest(POST, url, null))
    }
}
