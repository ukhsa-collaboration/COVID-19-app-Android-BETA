/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonRequest
import io.mockk.mockk
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject

class TestQueue : RequestQueue(mockk(), mockk()) {

    private val _requests = mutableListOf<Request<*>>()
    val requests: List<Request<*>> = _requests

    override fun <T : Any> add(request: Request<T>): Request<T> {
        _requests.add(request as Request<*>)
        return request
    }

    val lastRequest: Request<*>
        get() = requests.last()

    fun returnSuccess(json: JSONObject) =
        when (val request = requests.last()) {
            is SignableJsonObjectRequest -> {
                // Ideally we would have access to this function without having to rely on reflection.
                // Please don't do this in production code.
                val requestClass = JsonRequest::class.java
                val method = requestClass.getDeclaredMethod("deliverResponse", Object::class.java)
                method.isAccessible = true
                method.invoke(request, json)
            }
            else -> fail("Cannot return success on request $request")
        }

    fun returnError(exception: VolleyError) =
        requests.last().deliverError(exception)
}
