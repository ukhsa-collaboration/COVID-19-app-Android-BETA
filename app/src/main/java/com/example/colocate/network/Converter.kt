package com.example.colocate.network

import com.example.colocate.persistence.ContactEvent
import org.json.JSONArray
import org.json.JSONObject

fun convert(contactEvents: Iterable<ContactEvent>): JSONArray {
    return JSONArray(contactEvents.map(::convert))
}

fun convert(contactEvent: ContactEvent): JSONObject {
    return JSONObject().apply { 
        put("remoteContactId", contactEvent.remoteContactId)
        put("rssi", contactEvent.rssi)
        put("timestamp", contactEvent.timestamp)
    }
    
}