package uk.nhs.nhsx.sonar.android.client.http.volley

import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

open class UnsignedJsonObjectRequest(
    method: Int,
    url: String,
    body: JSONObject?,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(method, url, body, listener, errorListener) {

    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject?>? =
        if (response.data.isEmpty()) {
            Response.success<JSONObject>(
                JSONObject(),
                HttpHeaderParser.parseCacheHeaders(
                    response
                )
            )
        } else {
            super.parseNetworkResponse(response)
        }

    override fun getHeaders(): Map<String?, String?> =
        mapOf("Accept" to "application/json")
}
