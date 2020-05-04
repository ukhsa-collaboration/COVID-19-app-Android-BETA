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
            is EmberState -> jsonOf(
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

    fun deserialize(json: String): UserState? {
        val jsonObj = JSONObject(json)

        return when (jsonObj.getString("type")) {
            "EmberState" -> jsonObj.getEmberState()
            "RedState" -> jsonObj.getRedState()
            "CheckinState" -> jsonObj.getCheckinState()
            "RecoveryState" -> RecoveryState
            else -> DefaultState
        }
    }

    private fun JSONObject.getEmberState() =
        EmberState(getUntil())

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
