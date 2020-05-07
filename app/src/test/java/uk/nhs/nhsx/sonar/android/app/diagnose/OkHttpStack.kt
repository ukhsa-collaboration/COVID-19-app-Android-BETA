package uk.nhs.nhsx.sonar.android.app.diagnose

import com.android.volley.Header
import com.android.volley.Request
import com.android.volley.Request.Method.DELETE
import com.android.volley.Request.Method.GET
import com.android.volley.Request.Method.HEAD
import com.android.volley.Request.Method.OPTIONS
import com.android.volley.Request.Method.PATCH
import com.android.volley.Request.Method.POST
import com.android.volley.Request.Method.PUT
import com.android.volley.Request.Method.TRACE
import com.android.volley.toolbox.HttpResponse
import com.android.volley.toolbox.HurlStack
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.headersContentLength

class OkHttpStack : HurlStack() {
    private val client = OkHttpClient()

    override fun executeRequest(
        request: Request<*>,
        additionalHeaders: Map<String, String>
    ): HttpResponse {
        val okRequest = Builder().url(request.url)
            .headers(request.headers.plus(additionalHeaders).toHeaders())
            .methodAndBodyOf(request).build()

        val okResponse: Response = client.newCall(okRequest).execute()
        val headers = okResponse.headers.map { Header(it.first, it.second) }

        return if (okResponse.body != null) {
            HttpResponse(
                okResponse.code,
                headers,
                okResponse.headersContentLength().toInt(),
                okResponse.body!!.byteStream()
            )
        } else {
            HttpResponse(okResponse.code, headers)
        }
    }

    private fun Builder.methodAndBodyOf(
        request: Request<*>
    ): Builder {
        when (request.method) {
            GET -> get()
            DELETE -> delete()
            POST -> post(request.okRequestBody())
            PUT -> put(request.okRequestBody())
            HEAD -> head()
            OPTIONS -> method("OPTIONS", null)
            TRACE -> method("TRACE", null)
            PATCH -> patch(request.okRequestBody())
            else -> throw IllegalStateException("Unknown method ${request.method}")
        }

        return this
    }

    private fun Request<*>.okRequestBody(): RequestBody {
        val body = body ?: ByteArray(0)
        return body.toRequestBody(bodyContentType.toMediaTypeOrNull())
    }
}
