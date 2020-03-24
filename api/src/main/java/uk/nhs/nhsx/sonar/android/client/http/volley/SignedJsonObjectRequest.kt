package uk.nhs.nhsx.sonar.android.client.http.volley

import android.util.Base64
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

open class SignedJsonObjectRequest(
    private val key: ByteArray,
    method: Int,
    url: String,
    request: JSONObject,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(method, url, request, listener, errorListener) {

    private val bodyAsString: String = request.toString()

    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject?>? {
        return if (response.data.isEmpty()) {
            Response.success<JSONObject>(
                JSONObject(),
                HttpHeaderParser.parseCacheHeaders(
                    response
                )
            )
        } else {
            super.parseNetworkResponse(response)
        }
    }

    override fun getHeaders(): Map<String?, String?> {
        val headers = HashMap<String?, String?>()
        headers["Accept"] = "application/json"
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