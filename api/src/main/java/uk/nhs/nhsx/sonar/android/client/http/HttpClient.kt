/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.http

import org.json.JSONObject
import java.lang.Exception

data class HttpRequest(val urlPath: String, val json: JSONObject, val key: ByteArray? = null)

interface HttpClient {
    fun post(
        request: HttpRequest,
        onSuccess: (JSONObject) -> Unit,
        onError: (Exception) -> Unit
    )

    fun patch(
        request: HttpRequest,
        onSuccess: (JSONObject?) -> Unit,
        onError: (Exception) -> Unit
    )
}
