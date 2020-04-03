/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.http

import org.json.JSONObject

class HttpRequest(
    val urlPath: String,
    val json: JSONObject,
    val key: ByteArray? = null
)

interface HttpClient {
    fun post(
        request: HttpRequest,
        onSuccess: Callback<JSONObject>,
        onError: ErrorCallback
    )

    fun patch(
        request: HttpRequest,
        onSuccess: Callback<JSONObject?>,
        onError: ErrorCallback
    )
}

typealias Callback<T> = (T) -> Unit
typealias ErrorCallback = Callback<Exception>
typealias SimpleCallback = () -> Unit

fun jsonObjectOf(vararg pairs: Pair<String, Any>): JSONObject =
    JSONObject(mapOf(*pairs))

fun jsonOf(vararg pairs: Pair<String, Any>): String =
    jsonObjectOf(*pairs).toString()
