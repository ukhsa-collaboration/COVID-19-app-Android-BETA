package com.example.colocate.persistence

import com.example.colocate.di.module.PersistenceModule
import org.json.JSONArray
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationData
import javax.inject.Inject
import javax.inject.Named

class CoLocationDataProvider @Inject constructor(
    @Named(PersistenceModule.USE_CONNECTION_V2) private val useConnectionV2: Boolean,
    private val contactEventDao: ContactEventDao,
    private val contactEventV2Dao: ContactEventV2Dao,
    private val residentIdProvider: ResidentIdProvider
) {
    suspend fun getData(): CoLocationData {
        return if (useConnectionV2) {
            val events: JSONArray = convertV2(contactEventV2Dao.getAll())
            CoLocationData(residentIdProvider.getResidentId(), events)
        } else {
            val events: JSONArray = convert(contactEventDao.getAll())
            CoLocationData(residentIdProvider.getResidentId(), events)
        }
    }

    private fun convert(contactEvents: Iterable<ContactEvent>): JSONArray {
        return JSONArray(contactEvents.map(::convert))
    }

    private fun convertV2(contactEvents: Iterable<ContactEventV2>): JSONArray {
        return JSONArray(contactEvents.map(::convert))
    }

    private fun convert(contactEvent: ContactEvent): JSONObject {
        return JSONObject().apply {
            put("remoteContactId", contactEvent.remoteContactId)
            put("rssi", contactEvent.rssi)
            put("timestamp", contactEvent.timestamp)
        }
    }

    private fun convert(contactEvent: ContactEventV2): JSONObject {
        return JSONObject().apply {
            put("sonarId", contactEvent.sonarId)
            put("rssiValues", JSONArray(contactEvent.rssiValues.toTypedArray()))
            put("timestamp", contactEvent.timestamp)
            put("duration", contactEvent.duration)
        }
    }
}
