/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise.Deferred
import java.util.Base64

class SignableJsonObjectRequestTest {

    @Test
    fun `test getHeaders() for a signed request`() {
        val request = SignableJsonObjectRequest(
            HttpRequest(
                method = HttpMethod.POST,
                url = "http://somehere.com",
                jsonBody = JSONObject(),
                secretKey = generateSignatureKey()
            ),
            Deferred(),
            sonarHeaderValue = "someValue"
        ) { Base64.getEncoder().encodeToString(it) }

        val headers = request.headers
        assertThat(headers).containsKey("Sonar-Message-Signature")
        assertThat(headers).containsKey("Sonar-Request-Timestamp")
        assertThat(headers["Sonar-Request-Timestamp"]).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")
        assertThat(headers).containsEntry("X-Sonar-Foundation", "someValue")
    }

    @Test
    fun `test signing of request with no body`() {
        val request = SignableJsonObjectRequest(
            HttpRequest(
                method = HttpMethod.POST,
                url = "http://somehere.com",
                jsonBody = null,
                secretKey = generateSignatureKey()
            ),
            Deferred(),
            sonarHeaderValue = "someValue"
        ) { Base64.getEncoder().encodeToString(it) }

        val headers = request.headers
        assertThat(headers).containsKey("Sonar-Message-Signature")
        assertThat(headers).containsKey("Sonar-Request-Timestamp")
    }
}
