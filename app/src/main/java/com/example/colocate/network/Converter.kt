/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.network

import com.example.colocate.persistence.ContactEvent
import com.example.colocate.persistence.ContactEventV2
import org.json.JSONArray
import org.json.JSONObject

fun convert(contactEvents: Iterable<ContactEvent>): JSONArray {
    return JSONArray(contactEvents.map(::convert))
}

fun convertV2(contactEvents: Iterable<ContactEventV2>): JSONArray {
    return JSONArray(contactEvents.map(::convert))
}

fun convert(contactEvent: ContactEvent): JSONObject {
    return JSONObject().apply {
        put("remoteContactId", contactEvent.remoteContactId)
        put("rssi", contactEvent.rssi)
        put("timestamp", contactEvent.timestamp)
    }
}

fun convert(contactEvent: ContactEventV2): JSONObject {
    return JSONObject().apply {
        put("sonarId", contactEvent.sonarId)
        put("rssiValues", JSONArray(contactEvent.rssiValues.toTypedArray()))
        put("timestamp", contactEvent.timestamp)
        put("duration", contactEvent.duration)
    }
}
