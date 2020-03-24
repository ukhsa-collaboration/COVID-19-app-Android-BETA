/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.http.volley

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest


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
        val jsonObjectRequest = createRequest(Request.Method.POST, request.urlPath, request.json, onSuccess, onError)
        defineRetryPolicy(jsonObjectRequest)
        queue.add(jsonObjectRequest)
    }

    override fun patch(
        request: HttpRequest,
        onSuccess: (JSONObject?) -> Unit,
        onError: (java.lang.Exception) -> Unit
    ) {
        val jsonObjectRequest = createSignedRequest(request.key!!, Request.Method.PATCH, request.urlPath, request.json, onSuccess, onError)
        defineRetryPolicy(jsonObjectRequest)
        queue.add(jsonObjectRequest)
    }

    private fun createRequest(
        method: Int,
        urlPath: String,
        payload: JSONObject,
        onSuccess: (JSONObject) -> Unit,
        onError: (Exception) -> Unit
    ): JsonObjectRequest {
        return UnsignedJsonObjectRequest(method, url + urlPath, payload,
            Response.Listener { response ->
                onSuccess(response)
            },
            Response.ErrorListener { error ->
                onError(error)
            })
    }

    private fun createSignedRequest(
        key: ByteArray,
        method: Int,
        urlPath: String,
        payload: JSONObject,
        onSuccess: (JSONObject) -> Unit,
        onError: (Exception) -> Unit
    ): JsonObjectRequest {
        return SignedJsonObjectRequest(key, method, url + urlPath, payload,
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