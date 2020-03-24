/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.http.volley

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.nhaarman.mockitokotlin2.argumentCaptor
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.test.SignatureUtils

class VolleyHttpClientShould {

    @Test
    fun addJsonRequestToQueueWhenPost() {
        val queue = mock(RequestQueue::class.java)
        val url = "http://localhost:123"
        val cut =
            VolleyHttpClient(
                url,
                queue
            )

        cut.post(HttpRequest("/api", JSONObject()), {}) {
        }

        val requestCaptor = argumentCaptor<JsonObjectRequest>()
        verify(queue).add(requestCaptor.capture())
        val request = requestCaptor.firstValue
        assertThat(request.method).isEqualTo(Request.Method.POST)
        assertThat(request.url).isEqualTo("$url/api")
        assertThat(request.body).isEqualTo("{}".toByteArray())
        assertThat(request.bodyContentType).contains("application/json")
        assertThat(request.headers).containsEntry("Accept", "application/json")
    }

    @Test
    fun addJsonRequestToQueueWhenPatch() {
        val queue = mock(RequestQueue::class.java)
        val url = "http://localhost:123"
        val cut =
            VolleyHttpClient(
                url,
                queue
            )

        cut.patch(HttpRequest("/api", JSONObject(), SignatureUtils.generateKey()), {}) {
        }

        val requestCaptor = argumentCaptor<JsonObjectRequest>()
        verify(queue).add(requestCaptor.capture())
        val request = requestCaptor.firstValue
        assertThat(request.method).isEqualTo(Request.Method.PATCH)
        assertThat(request.url).isEqualTo("$url/api")
        assertThat(request.body).isEqualTo("{}".toByteArray())
        assertThat(request.bodyContentType).contains("application/json")
        assertThat(request.headers).containsEntry("Accept", "application/json")
    }
}
