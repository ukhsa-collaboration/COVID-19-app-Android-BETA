/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.HttpMethod.PUT
import uk.nhs.nhsx.sonar.android.app.http.HttpRequest
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf

class ReferenceCodeApi(
    private val baseUrl: String,
    private val secretKeyStorage: SecretKeyStorage,
    private val httpClient: HttpClient
) {

    fun get(sonarId: String): Promise<ReferenceCode> {
        val secretKey = secretKeyStorage.provideSecretKey()
        val url = "$baseUrl/api/app-instances/linking-id"

        return httpClient
            .send(HttpRequest(PUT, url, jsonObjectOf("sonarId" to sonarId), secretKey))
            .map { ReferenceCode(it.getString("linkingId")) }
    }
}
