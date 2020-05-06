/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.app.http.jsonOf
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet

object UserStateSerialization {

    fun serialize(state: UserState): String =
        when (state) {
            is DefaultState -> jsonOf(
                "type" to state.type()
            )
            is RecoveryState -> jsonOf(
                "type" to state.type()
            )
            is AmberState -> jsonOf(
                "type" to state.type(),
                "until" to state.until.millis
            )
            is RedState -> jsonOf(
                "type" to state.type(),
                "until" to state.until.millis,
                "symptoms" to state.symptoms.map { it.toString() }
            )
            is CheckinState -> jsonOf(
                "type" to state.type(),
                "until" to state.until.millis,
                "symptoms" to state.symptoms.map { it.toString() }
            )
        }

    fun deserialize(json: String?): UserState {
        if (json == null) return DefaultState

        val jsonObj = JSONObject(json)

        val deserialized =
            when (jsonObj.getString("type")) {
                "AmberState", "EmberState" -> jsonObj.getAmberState()
                "RedState" -> jsonObj.getRedState()
                "CheckinState" -> jsonObj.getCheckinState()
                "RecoveryState" -> RecoveryState
                else -> DefaultState
            }

        return deserialized ?: DefaultState
    }

    private fun JSONObject.getAmberState() =
        AmberState(getUntil())

    private fun JSONObject.getRedState() =
        getSymptoms()?.let { RedState(getUntil(), it) }

    private fun JSONObject.getCheckinState() =
        getSymptoms()?.let { CheckinState(getUntil(), it) }

    private fun UserState.type() =
        javaClass.simpleName

    private fun JSONObject.getUntil(): DateTime =
        DateTime(getLong("until"), DateTimeZone.UTC)

    private fun JSONObject.getSymptoms(): NonEmptySet<Symptom>? {
        val array = getJSONArray("symptoms")
        val symptoms = mutableSetOf<Symptom>()

        for (index in 0 until array.length()) {
            val stringValue = array.getString(index)
            val symptom = Symptom.valueOf(stringValue)
            symptoms.add(symptom)
        }

        return NonEmptySet.create(symptoms)
    }
}
