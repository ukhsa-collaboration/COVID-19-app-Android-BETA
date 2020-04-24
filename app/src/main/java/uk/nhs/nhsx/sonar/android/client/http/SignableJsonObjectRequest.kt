/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client.http

import com.android.volley.NetworkResponse
import com.android.volley.Request.Method
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.Promise.Deferred
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SignableJsonObjectRequest(
    private val httpRequest: HttpRequest,
    deferred: Deferred<JSONObject>,
    private val sonarHeaderValue: String,
    private val base64enc: (ByteArray) -> String
) :
    JsonObjectRequest(
        httpRequest.method.toInt(),
        httpRequest.url,
        httpRequest.jsonBody,
        Response.Listener { deferred.resolve(it) },
        Response.ErrorListener { deferred.fail(it) }
    ) {

    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> =
        if (response.data.isEmpty()) {
            Response.success(JSONObject(), HttpHeaderParser.parseCacheHeaders(response))
        } else {
            super.parseNetworkResponse(response)
        }

    override fun getHeaders(): Map<String, String> {
        val defaultHeaders = arrayOf(
            "Accept" to "application/json",
            "X-Sonar-Foundation" to sonarHeaderValue
        )

        return when (httpRequest.key) {
            null -> return mapOf(*defaultHeaders)
            else -> {
                val timestampAsString = LocalDateTime
                    .now(DateTimeZone.UTC)
                    .toString("yyyy-MM-dd'T'HH:mm:ss'Z'")

                val signature = generateSignature(httpRequest.key, timestampAsString, body)

                mapOf(
                    *defaultHeaders,
                    "Sonar-Request-Timestamp" to timestampAsString,
                    "Sonar-Message-Signature" to signature
                )
            }
        }
    }

    private fun generateSignature(key: ByteArray, timestamp: String, body: ByteArray): String {
        val secretKey: SecretKey = SecretKeySpec(key, "HMACSHA256")
        val mac = Mac.getInstance("HMACSHA256")
            .apply {
                init(secretKey)
                update(timestamp.toByteArray(Charsets.UTF_8))
            }

        val signature = mac.doFinal(body)

        return base64enc(signature)
    }
}

private fun HttpMethod.toInt() =
    when (this) {
        HttpMethod.GET -> Method.GET
        HttpMethod.POST -> Method.POST
        HttpMethod.PATCH -> Method.PATCH
        HttpMethod.PUT -> Method.PUT
        HttpMethod.DELETE -> Method.DELETE
    }
