/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.colocation

import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest

class CoLocationApi(private val key: ByteArray, private val httpClient: HttpClient) {

    fun save(coLocationData: CoLocationData, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        val request = HttpRequest(
            "/api/residents/${coLocationData.residentId}",
            coLocationData.contactEvents,
            key
        )
        httpClient.patch(request, { onSuccess() }, { exception -> onError(exception) })
    }
}
