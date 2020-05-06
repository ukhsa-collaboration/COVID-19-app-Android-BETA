/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import com.android.volley.Request
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import java.nio.charset.Charset

fun Request<*>.assertBodyHasJson(vararg fields: Pair<String, Any>) {
    assertThat(bodyContentType).contains("application/json")
    assertThat(body.toString(Charset.defaultCharset())).isEqualTo(JSONObject(mapOf(*fields)).toString())
}
