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

    override fun getHeaders(): Map<String?, String?> {
        val timestampAsString = LocalDateTime.now(
            DateTimeZone.UTC
        ).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")

        val signature: String? = generateSignature(
            key,
            timestampAsString,
            body
        )

        return super.getHeaders() + mapOf(
            "Sonar-Request-Timestamp" to timestampAsString,
            "Sonar-Message-Signature" to signature
        )
    }

    private fun generateSignature(key: ByteArray, timestamp: String, body: ByteArray): String? {
        val secretKey: SecretKey = SecretKeySpec(key, "HMACSHA256")
        val mac: Mac = Mac.getInstance("HMACSHA256")
        mac.init(secretKey)
        mac.update(timestamp.toByteArray(Charsets.UTF_8))
        val signature = mac.doFinal(body)

        return Base64.encodeToString(signature, Base64.DEFAULT)
    }
}
