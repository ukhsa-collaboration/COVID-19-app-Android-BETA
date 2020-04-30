/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.HttpMethod.PUT
import uk.nhs.nhsx.sonar.android.app.http.HttpRequest
import uk.nhs.nhsx.sonar.android.app.http.Promise
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider

class ReferenceCodeApi(
    private val baseUrl: String,
    private val sonarIdProvider: SonarIdProvider,
    private val secretKeyStorage: SecretKeyStorage,
    private val httpClient: HttpClient
) {

    fun generate(): Promise<ReferenceCode> {
        val secretKey = secretKeyStorage.provideSecretKey()
        val sonarId = sonarIdProvider.getSonarId()
        val url = "$baseUrl/api/residents/$sonarId/linking-id"

        return httpClient
            .send(HttpRequest(PUT, url, jsonObjectOf(), secretKey))
            .map { ReferenceCode(it.getString("linkingId")) }
    }
}
