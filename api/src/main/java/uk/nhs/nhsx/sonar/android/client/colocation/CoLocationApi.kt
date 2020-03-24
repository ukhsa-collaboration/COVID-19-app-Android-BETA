/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.colocation

import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest

class CoLocationApi(private val key: ByteArray, private val httpClient: HttpClient) {

    fun save(coLocationData: CoLocationData, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        httpClient.patch(HttpRequest("/api/residents/" + coLocationData.residentId, JSONObject(), key), {
                onSuccess()
        }, {
                error: java.lang.Exception -> onError(error)
        })
    }
}
