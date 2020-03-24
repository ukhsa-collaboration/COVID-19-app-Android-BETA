package uk.nhs.nhsx.sonar.android.client.http.volley

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

object RequestQueueFactory {
    fun createQueue(
        context: Context
    ): RequestQueue {
        return Volley.newRequestQueue(context)
    }
}
