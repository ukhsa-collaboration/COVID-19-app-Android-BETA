/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import com.android.volley.NetworkResponse
import com.android.volley.Request.Method
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise.Deferred
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise.State.Explanation
import javax.crypto.Mac
import javax.crypto.SecretKey

class SignableJsonObjectRequest(
    private val httpRequest: HttpRequest,
    deferred: Deferred<JSONObject>,
    private val sonarHeaderValue: String,
    val appVersion: String,
    private val utcClock: UTCClock = RealUTCClock(),
    private val base64enc: (ByteArray) -> String
) :
    JsonObjectRequest(
        httpRequest.method.toInt(),
        httpRequest.url,
        httpRequest.jsonBody,
        Response.Listener { deferred.resolve(it) },
        Response.ErrorListener { deferred.failWithVolleyError(it) }
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
            "X-Sonar-Foundation" to sonarHeaderValue,
            "X-Sonar-App-Version" to appVersion
        )

        return when (httpRequest.secretKey) {
            null -> return mapOf(*defaultHeaders)
            else -> {
                val timestampAsString = utcClock.now()
                    .toString("yyyy-MM-dd'T'HH:mm:ss'Z'")

                val signature = generateSignature(
                    httpRequest.secretKey,
                    timestampAsString,
                    body ?: ByteArray(0)
                )

                mapOf(
                    *defaultHeaders,
                    "Sonar-Request-Timestamp" to timestampAsString,
                    "Sonar-Message-Signature" to signature
                )
            }
        }
    }

    private fun generateSignature(
        secretKey: SecretKey,
        timestamp: String,
        body: ByteArray
    ): String {
        val mac = Mac.getInstance("HMACSHA256")
            .apply {
                init(secretKey)
                update(timestamp.toByteArray(Charsets.UTF_8))
            }

        val signature = mac.doFinal(body)

        return base64enc(signature)
    }
}

fun <T> Deferred<T>.failWithVolleyError(error: VolleyError) =
    fail(Explanation(error.message ?: "HttpClient error", error, error.networkResponse?.statusCode))

private fun HttpMethod.toInt() =
    when (this) {
        HttpMethod.GET -> Method.GET
        HttpMethod.POST -> Method.POST
        HttpMethod.PATCH -> Method.PATCH
        HttpMethod.PUT -> Method.PUT
        HttpMethod.DELETE -> Method.DELETE
    }
