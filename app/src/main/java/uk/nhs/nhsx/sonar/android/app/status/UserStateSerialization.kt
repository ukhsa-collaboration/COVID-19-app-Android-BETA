/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.app.http.jsonOf
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NO_DAYS_IN_AMBER
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NO_DAYS_IN_RED
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
                "since" to state.since.millis,
                "until" to state.until.millis
            )
            is RedState -> jsonOf(
                "type" to state.type(),
                "since" to state.since.millis,
                "until" to state.until.millis,
                "symptoms" to state.symptoms.map { it.value }
            )
            is CheckinState -> jsonOf(
                "type" to state.type(),
                "since" to state.since.millis,
                "until" to state.until.millis,
                "symptoms" to state.symptoms.map { it.value }
            )
        }

    fun deserialize(json: String?): UserState {
        if (json == null) return DefaultState

        val jsonObj = JSONObject(json)

        return when (jsonObj.getString("type")) {
            "AmberState", "EmberState" -> jsonObj.getAmberState()
            "RedState" -> jsonObj.getRedState()
            "CheckinState" -> jsonObj.getCheckinState()
            "RecoveryState" -> RecoveryState
            else -> DefaultState
        } ?: DefaultState
    }

    private fun JSONObject.getAmberState(): AmberState {
        val until = getUntil()
        val since = getSince() ?: until.minusDays(NO_DAYS_IN_AMBER)
        return AmberState(since, until)
    }

    private fun JSONObject.getRedState(): RedState? {
        return getSymptoms()?.let { symptoms ->
            val until = getUntil()
            val since = getSince() ?: until.minusDays(NO_DAYS_IN_RED)
            RedState(since, getUntil(), symptoms)
        }
    }

    private fun JSONObject.getCheckinState(): CheckinState? {
        return getSymptoms()?.let {
            val until = getUntil()
            val since = getSince() ?: until.minusDays(NO_DAYS_IN_RED)
            CheckinState(since, until, it)
        }
    }

    private fun UserState.type() = javaClass.simpleName

    private fun JSONObject.getUntil(): DateTime =
        getLongOrNull("until")?.let {
            DateTime(it, DateTimeZone.UTC)
        } ?: DateTime.now(DateTimeZone.UTC)

    private fun JSONObject.getSince(): DateTime? {
        return getLongOrNull("since")?.let {
            DateTime(it, DateTimeZone.UTC)
        }
    }

    private fun JSONObject.getSymptoms(): NonEmptySet<Symptom>? {
        //  TODO("fix this to handle null symptoms")
        val array = getJSONArray("symptoms")
        val symptoms = mutableSetOf<Symptom>()

        for (index in 0 until array.length()) {
            val stringValue = array.getString(index)
            val symptom = Symptom.fromValue(stringValue) ?: continue
            symptoms.add(symptom)
        }

        return NonEmptySet.create(symptoms)
    }

    private fun JSONObject.getLongOrNull(key: String): Long? {
        return if (has(key)) {
            val value: Long = optLong(key, -1L)
            if (value == -1L) return null
            else value
        } else null
    }
}
