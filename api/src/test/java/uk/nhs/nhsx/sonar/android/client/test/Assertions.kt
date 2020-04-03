package uk.nhs.nhsx.sonar.android.client.test

import com.android.volley.Request
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import java.nio.charset.Charset

fun Request<*>.assertBodyHasJson(json: Map<String, Any>) {
    assertThat(bodyContentType).contains("application/json")
    assertThat(body.toString(Charset.defaultCharset())).isEqualTo(JSONObject(json).toString())
}
