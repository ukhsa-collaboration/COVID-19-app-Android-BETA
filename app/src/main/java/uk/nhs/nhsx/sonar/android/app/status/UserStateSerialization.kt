/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.app.http.jsonOf
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NO_DAYS_IN_RED
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet

object UserStateSerialization {

    fun serialize(state: UserState): String =
        when (state) {
            is DefaultState -> jsonOf(
                "type" to state.type(),
                "testInfo" to state.testInfo.serialize()
            )
            is RecoveryState -> jsonOf(
                "type" to state.type(),
                "testInfo" to state.testInfo.serialize()
            )
            is AmberState -> jsonOf(
                "type" to state.type(),
                "until" to state.until.millis,
                "testInfo" to state.testInfo.serialize()
            )
            is RedState -> jsonOf(
                "type" to state.type(),
                "since" to state.since.serialize(),
                "until" to state.until.millis,
                "symptoms" to state.symptoms.map { it.value },
                "testInfo" to state.testInfo.serialize()
            )
            is CheckinState -> jsonOf(
                "type" to state.type(),
                "since" to state.since.serialize(),
                "until" to state.until.millis,
                "symptoms" to state.symptoms.map { it.value },
                "testInfo" to state.testInfo.serialize()
            )
        }

    private fun TestInfo?.serialize(): String =
        this?.let {
            jsonOf(
                "testResult" to testResult,
                "stateChanged" to stateChanged,
                "dismissed" to dismissed,
                "testDate" to testDate.millis
            )
        } ?: "null"

    private fun DateTime?.serialize(): Long =
        this?.let {
            millis
        } ?: -1

    fun deserialize(json: String?): UserState {
        if (json == null) return DefaultState()

        val jsonObj = JSONObject(json)

        val deserialized =
            when (jsonObj.getString("type")) {
                "AmberState", "EmberState" -> jsonObj.getAmberState()
                "RedState" -> jsonObj.getRedState()
                "CheckinState" -> jsonObj.getCheckinState()
                "RecoveryState" -> jsonObj.getRecoveryState()
                else -> jsonObj.getDefaultState()
            }

        return deserialized ?: DefaultState()
    }

    private fun JSONObject.getDefaultState() =
        DefaultState(getTestInfo())

    private fun JSONObject.getRecoveryState() =
        RecoveryState(getTestInfo())

    private fun JSONObject.getAmberState() =
        AmberState(getUntil(), getTestInfo())

    private fun JSONObject.getRedState(): RedState? {
        return getSymptoms()?.let { symptoms ->
            val until = getUntil()
            val since = getSince() ?: until.minusDays(NO_DAYS_IN_RED)
            RedState(since, getUntil(), symptoms, getTestInfo())
        }
    }

    private fun JSONObject.getCheckinState(): CheckinState? {
        return getSymptoms()?.let {
            val until = getUntil()
            val since = getSince() ?: until.minusDays(NO_DAYS_IN_RED)
            CheckinState(since, until, it, getTestInfo())
        }
    }

    private fun UserState.type() =
        javaClass.simpleName

    private fun JSONObject.getUntil(): DateTime =
        getLongOrNull("until")?.let {
            DateTime(it, DateTimeZone.UTC)
        } ?: DateTime.now(DateTimeZone.UTC)

    private fun JSONObject.getSince(): DateTime? {
        return getLongOrNull("since")?.let {
            DateTime(it, DateTimeZone.UTC)
        }
    }

    private fun JSONObject.getTestInfo(): TestInfo? =
        getStringOrNull("testInfo")?.let {
            val json = JSONObject(it)
            val dismissed = json.optBoolean("dismissed", true)
            val stateChanged = json.optBoolean("stateChanged", false)
            val result = TestResult.valueOf(json.optString("testResult", "INVALID"))
            val testDate = json.getDateTime("testDate")

            TestInfo(result, testDate, stateChanged, dismissed)
        }

    private fun JSONObject.getDateTime(key: String): DateTime =
        getLongOrNull(key)?.let {
            DateTime(it, DateTimeZone.UTC)
        } ?: DateTime.now(DateTimeZone.UTC)

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

    private fun JSONObject.getStringOrNull(key: String): String? {
        return if (has(key)) {
            val value: String = optString(key, "null")
            if (value == "null") return null
            else value
        } else null
    }
}
