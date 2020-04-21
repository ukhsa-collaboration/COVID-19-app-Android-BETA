/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client

import com.android.volley.Request.Method.PUT
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.TestQueue

class AcknowledgmentsApiTest {

    private val requestQueue = TestQueue()
    private val httpClient = HttpClient(requestQueue)
    private val acknowledgmentsApi = AcknowledgmentsApi(httpClient)

    @Test
    fun `test send()`() {
        acknowledgmentsApi.send("https://api.example.com/ack/10012")

        val request = requestQueue.lastRequest
        assertThat(request.method).isEqualTo(PUT)
        assertThat(request.url).isEqualTo("https://api.example.com/ack/10012")
    }
}
