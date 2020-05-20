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
                "type" to state.type(),
                "testResult" to state.testResult.serialize()
            )
            is RecoveryState -> jsonOf(
                "type" to state.type(),
                "testResult" to state.testResult.serialize()
            )
            is AmberState -> jsonOf(
                "type" to state.type(),
                "until" to state.until.millis,
                "testResult" to state.testResult.serialize()
            )
            is RedState -> jsonOf(
                "type" to state.type(),
                "until" to state.until.millis,
                "symptoms" to state.symptoms.map { it.value },
                "symptomsStartDate" to state.symptomsStartDate.serialize(),
                "testResult" to state.testResult.serialize()
            )
            is CheckinState -> jsonOf(
                "type" to state.type(),
                "until" to state.until.millis,
                "symptoms" to state.symptoms.map { it.value },
                "symptomsStartDate" to state.symptomsStartDate.serialize(),
                "testResult" to state.testResult.serialize()
            )
        }

    private fun TestResult?.serialize(): String =
        this?.let {
            jsonOf(
                "result" to result,
                "stateChanged" to stateChanged,
                "dismissed" to dismissed
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
        DefaultState(getTestResult())

    private fun JSONObject.getRecoveryState() =
        RecoveryState(getTestResult())

    private fun JSONObject.getAmberState() =
        AmberState(getUntil(), getTestResult())

    private fun JSONObject.getRedState(): RedState? {
        return getSymptoms()?.let { symptoms ->
            RedState(getUntil(), symptoms, getSymptomsDate(), getTestResult())
        }
    }

    private fun JSONObject.getCheckinState() =
        getSymptoms()?.let { CheckinState(getUntil(), it, getSymptomsDate(), getTestResult()) }

    private fun UserState.type() =
        javaClass.simpleName

    private fun JSONObject.getUntil(): DateTime =
        getLongOrNull("until")?.let {
            DateTime(it, DateTimeZone.UTC)
        } ?: DateTime.now(DateTimeZone.UTC)

    private fun JSONObject.getSymptomsDate(): DateTime? =
        getLongOrNull("symptomsStartDate")?.let {
            DateTime(it, DateTimeZone.UTC)
        }

    private fun JSONObject.getTestResult(): TestResult? =
        getStringOrNull("testResult")?.let {
            val json = JSONObject(it)
            val dismissed = json.optBoolean("dismissed", true)
            val stateChanged = json.optBoolean("stateChanged", false)
            val result = json.optString("result", "INVALID")
            TestResult(result, stateChanged, dismissed)
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

    private fun JSONObject.getStringOrNull(key: String): String? {
        return if (has(key)) {
            val value: String = optString(key, "null")
            if (value == "null") return null
            else value
        } else null
    }
}
