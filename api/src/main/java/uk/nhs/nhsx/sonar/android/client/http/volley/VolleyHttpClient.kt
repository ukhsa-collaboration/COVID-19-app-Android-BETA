/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.http.volley

import android.content.Context
import android.util.Base64
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.HttpRequest
import uk.nhs.nhsx.sonar.android.client.http.Promise
import uk.nhs.nhsx.sonar.android.client.http.Promise.Deferred

class VolleyHttpClient(
    private val queue: RequestQueue,
    private val base64enc: (ByteArray) -> String = { Base64.encodeToString(it, Base64.DEFAULT) }
) : HttpClient {

    constructor(ctx: Context) : this(Volley.newRequestQueue(ctx))

    override fun send(request: HttpRequest): Promise<JSONObject> {
        val deferred = Deferred<JSONObject>()
        val volleyRequest = createRequest(request, deferred)

        queue.add(volleyRequest)

        return deferred.promise
    }

    private fun createRequest(request: HttpRequest, deferred: Deferred<JSONObject>): JsonObjectRequest =
        SignableJsonObjectRequest(request, deferred, base64enc).apply {
            retryPolicy = DefaultRetryPolicy(
                30 * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        }
}
