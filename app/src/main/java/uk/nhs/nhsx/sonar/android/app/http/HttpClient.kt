/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import android.util.Base64
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.NoCache
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise.Deferred
import javax.crypto.SecretKey

private fun createQueue(): RequestQueue =
    RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply { start() }

class HttpClient(
    private val queue: RequestQueue,
    private val sonarHeaderValue: String,
    private val utcClock: UTCClock = RealUTCClock(),
    private val base64enc: (ByteArray) -> String = { Base64.encodeToString(it, Base64.DEFAULT) }
) {
    constructor(sonarHeaderValue: String) : this(createQueue(), sonarHeaderValue)

    fun send(request: HttpRequest): Promise<JSONObject> {
        val deferred = Deferred<JSONObject>()
        val volleyRequest = createRequest(request, deferred)

        queue.add(volleyRequest)

        return deferred.promise
    }

    private fun createRequest(
        request: HttpRequest,
        deferred: Deferred<JSONObject>
    ): JsonObjectRequest =
        SignableJsonObjectRequest(
            httpRequest = request,
            deferred = deferred,
            base64enc = base64enc,
            utcClock = utcClock,
            sonarHeaderValue = sonarHeaderValue
        ).apply {
            retryPolicy = DefaultRetryPolicy(
                30 * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        }
}

class HttpRequest(
    val method: HttpMethod,
    val url: String,
    val jsonBody: JSONObject? = null,
    val secretKey: SecretKey? = null
)

enum class HttpMethod {
    GET,
    POST,
    PATCH,
    PUT,
    DELETE,
}

fun jsonObjectOf(vararg pairs: Pair<String, Any>): JSONObject =
    JSONObject(mapOf(*pairs))

fun jsonOf(vararg pairs: Pair<String, Any>): String =
    jsonObjectOf(*pairs).toString()
