/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client.colocation

import org.json.JSONArray
import org.json.JSONObject

data class CoLocationData(val residentId: String, val events: JSONArray) {
    val contactEvents: JSONObject
        get() = JSONObject().apply {
            put("contactEvents", events)
        }
}
