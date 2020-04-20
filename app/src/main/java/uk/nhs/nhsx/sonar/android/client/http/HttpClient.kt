/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client.http

import android.content.Context
import android.util.Base64
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.Promise.Deferred

class HttpClient(
    private val queue: RequestQueue,
    private val base64enc: (ByteArray) -> String = { Base64.encodeToString(it, Base64.DEFAULT) }
) {

    constructor(ctx: Context) : this(Volley.newRequestQueue(ctx))

    fun send(request: HttpRequest): Promise<JSONObject> {
        val deferred = Deferred<JSONObject>()
        val volleyRequest = createRequest(request, deferred)

        queue.add(volleyRequest)

        return deferred.promise
    }

    private fun createRequest(request: HttpRequest, deferred: Deferred<JSONObject>): JsonObjectRequest =
        SignableJsonObjectRequest(request, deferred, base64enc)
            .apply {
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
    val key: ByteArray? = null
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
