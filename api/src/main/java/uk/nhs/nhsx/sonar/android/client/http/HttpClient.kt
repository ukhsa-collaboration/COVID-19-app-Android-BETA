/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.http

import org.json.JSONObject

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

interface HttpClient {
    fun send(request: HttpRequest): Promise<JSONObject>
}

fun jsonObjectOf(vararg pairs: Pair<String, Any>): JSONObject =
    JSONObject(mapOf(*pairs))

fun jsonOf(vararg pairs: Pair<String, Any>): String =
    jsonObjectOf(*pairs).toString()
