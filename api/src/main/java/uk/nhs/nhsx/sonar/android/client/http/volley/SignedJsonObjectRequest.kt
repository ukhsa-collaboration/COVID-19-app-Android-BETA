/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.http.volley

import android.util.Base64
import com.android.volley.Response
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SignedJsonObjectRequest(
    private val key: ByteArray,
    method: Int,
    url: String,
    request: JSONObject,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener
) : UnsignedJsonObjectRequest(method, url, request, listener, errorListener) {

    private val bodyAsString: String = request.toString()

    override fun getHeaders(): Map<String?, String?> {
        val headers = super.getHeaders().toMutableMap()
        val timestampAsString = LocalDateTime.now(
            DateTimeZone.UTC
        ).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        headers["Sonar-Request-Timestamp"] = timestampAsString
        headers["Sonar-Request-Signature"] = generateSignature(
            key,
            timestampAsString + bodyAsString
        )
        return headers
    }

    private fun generateSignature(key: ByteArray, data: String): String? {
        val secretKey: SecretKey = SecretKeySpec(key, "HMACSHA256")
        val mac: Mac = Mac.getInstance("HMACSHA256")
        mac.init(secretKey)
        mac.update(data.toByteArray())
        return Base64.encodeToString(mac.doFinal(), Base64.DEFAULT)
    }
}
