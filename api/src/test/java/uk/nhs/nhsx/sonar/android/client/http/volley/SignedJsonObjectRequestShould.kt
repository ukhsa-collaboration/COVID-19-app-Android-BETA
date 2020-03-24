/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.http.volley

import com.android.volley.Request
import com.android.volley.Response
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.test.SignatureUtils

class SignedJsonObjectRequestShould {

    @Test
    fun containTimestampHeader() {
        val cut = SignedJsonObjectRequest(
            SignatureUtils.generateKey(),
            Request.Method.POST,
            "http://somehere.com",
            JSONObject(),
            Response.Listener {},
            Response.ErrorListener {}
        )

        assertThat(cut.headers).containsKey("Sonar-Request-Timestamp")
        assertThat(cut.headers["Sonar-Request-Timestamp"]).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")
    }

    @Test
    fun containSignatureHeader() {
        val cut = SignedJsonObjectRequest(
            SignatureUtils.generateKey(),
            Request.Method.POST,
            "http://somehere.com",
            JSONObject(),
            Response.Listener {},
            Response.ErrorListener {}
        )

        assertThat(cut.headers).containsKey("Sonar-Request-Signature")
    }
}
