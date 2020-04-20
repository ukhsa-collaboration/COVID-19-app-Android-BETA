/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client.http

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.http.Promise.Deferred
import java.util.Base64

class SignableJsonObjectRequestTest {

    @Test
    fun `test getHeaders() for a signed request`() {
        val request = SignableJsonObjectRequest(
            HttpRequest(
                method = HttpMethod.POST,
                url = "http://somehere.com",
                jsonBody = JSONObject(),
                key = generateSignatureKey()
            ),
            Deferred()
        ) { Base64.getEncoder().encodeToString(it) }

        assertThat(request.headers).containsKey("Sonar-Message-Signature")
        assertThat(request.headers).containsKey("Sonar-Request-Timestamp")
        assertThat(request.headers["Sonar-Request-Timestamp"]).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")
    }
}
