/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.http.volley

import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import kotlin.collections.HashMap


open class VolleyHttpClient(
    private val url: String,
    private val queue: RequestQueue
) : HttpClient {

    constructor(
        url: String,
        ctx: Context
    ) : this(
        url,
        RequestQueueFactory.createQueue(
            ctx
        )
    )

    override fun post(
        request: HttpRequest,
        onSuccess: (JSONObject) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val jsonObjectRequest = createRequest(Request.Method.POST, request.urlPath, onSuccess, onError)
        defineRetryPolicy(jsonObjectRequest)
        queue.add(jsonObjectRequest)
    }

    override fun patch(
        request: HttpRequest,
        onSuccess: (JSONObject?) -> Unit,
        onError: (java.lang.Exception) -> Unit
    ) {
        val jsonObjectRequest = createSignedRequest(request.key!!, Request.Method.PATCH, request.urlPath, onSuccess, onError)
        defineRetryPolicy(jsonObjectRequest)
        queue.add(jsonObjectRequest)
    }

    private fun createRequest(
        method: Int,
        urlPath: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (Exception) -> Unit
    ): JsonObjectRequest {
        return object : JsonObjectRequest(method, url + urlPath, JSONObject(),
            Response.Listener { response ->
                onSuccess(response)
            },
            Response.ErrorListener { error ->
                onError(error)
            })
        {
            override fun getHeaders(): Map<String?, String?> {
                val headers = HashMap<String?, String?>()
                headers["Accept"] = "application/json"
                return headers
            }
        }
    }

    private fun createSignedRequest(
        key: ByteArray,
        method: Int,
        urlPath: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (Exception) -> Unit
    ): JsonObjectRequest {
        return SignedJsonObjectRequest(key, method, url + urlPath, JSONObject(),
            Response.Listener { response ->
                onSuccess(response)
            },
            Response.ErrorListener { error ->
                onError(error)
            })
    }

    private fun defineRetryPolicy(request: JsonObjectRequest) {
        request.retryPolicy = DefaultRetryPolicy(
            30 * 1000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
    }
}