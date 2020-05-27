/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.app.http.jsonOf
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NO_DAYS_IN_EXPOSED
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NO_DAYS_IN_SYMPTOMATIC
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet

object UserStateSerialization {

    fun serialize(state: UserState): String =
        when (state) {
            is DefaultState -> jsonOf(
                "type" to state.type()
            )
            is ExposedState -> jsonOf(
                "type" to state.type(),
                "since" to state.since.millis,
                "until" to state.until.millis
            )
            is SymptomaticState -> jsonOf(
                "type" to state.type(),
                "since" to state.since.millis,
                "until" to state.until.millis,
                "symptoms" to state.symptoms.map { it.value }
            )
            is PositiveState -> jsonOf(
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
            "ExposedState", "AmberState", "EmberState" -> jsonObj.getExposedState()
            "SymptomaticState", "RedState", "CheckinState" -> jsonObj.getSymptomaticState()
            "PositiveState" -> jsonObj.getPositiveState()
            else -> DefaultState
        } ?: DefaultState
    }

    private fun JSONObject.getExposedState(): ExposedState {
        val until = getUntil()
        val since = getSince() ?: until.minusDays(NO_DAYS_IN_EXPOSED)
        return ExposedState(since, until)
    }

    private fun JSONObject.getSymptomaticState(): SymptomaticState? {
        val symptoms = getSymptoms()
        if (symptoms.isEmpty()) return null

        val until = getUntil()
        val since = getSince() ?: until.minusDays(NO_DAYS_IN_SYMPTOMATIC)
        return SymptomaticState(since, getUntil(), NonEmptySet.create(symptoms)!!)
    }

    private fun JSONObject.getPositiveState(): PositiveState? {
        val until = getUntil()
        val since = getSince() ?: until.minusDays(NO_DAYS_IN_SYMPTOMATIC)
        return PositiveState(since, getUntil(), getSymptoms())
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

    private fun JSONObject.getSymptoms(): Set<Symptom> {
        if (!has("symptoms")) return emptySet()

        val array = getJSONArray("symptoms")
        val symptoms = mutableSetOf<Symptom>()

        for (index in 0 until array.length()) {
            val stringValue = array.getString(index)
            val symptom = Symptom.fromValue(stringValue) ?: continue
            symptoms.add(symptom)
        }

        return symptoms
    }

    private fun JSONObject.getLongOrNull(key: String): Long? {
        return if (has(key)) {
            val value: Long = optLong(key, -1L)
            if (value == -1L) return null
            else value
        } else null
    }
}
