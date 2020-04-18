/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.lang.reflect.Type

sealed class UserState(private val type: String, @Transient open val until: DateTime) {

    fun serialize(): String = gSon.toJson(this)

    companion object {
        private val userStateTypeAdapterFactory: RuntimeTypeAdapterFactory<UserState> =
            RuntimeTypeAdapterFactory.of(UserState::class.java, "type")
                .registerSubtype(DefaultState::class.java, DefaultState::class.simpleName)
                .registerSubtype(EmberState::class.java, EmberState::class.simpleName)
                .registerSubtype(RedState::class.java, RedState::class.simpleName)

        private val gSon: Gson = GsonBuilder()
            .registerTypeAdapter(DateTime::class.java, DateTimeAdapter())
            .registerTypeAdapterFactory(userStateTypeAdapterFactory)
            .create()

        fun deserialize(json: String): UserState = gSon.fromJson(json, UserState::class.java)
    }

    override fun toString(): String = "UserState($type)"
}

data class DefaultState(override val until: DateTime) :
    UserState(DefaultState::class.simpleName!!, until)

data class EmberState(override val until: DateTime) :
    UserState(EmberState::class.simpleName!!, until)

data class RedState(override val until: DateTime, val symptoms: Set<Symptom>) :
    UserState(RedState::class.simpleName!!, until)

private class DateTimeAdapter : JsonSerializer<DateTime>, JsonDeserializer<DateTime> {
    override fun serialize(
        src: DateTime,
        typeOf: Type,
        context: JsonSerializationContext
    ) = JsonPrimitive(src.millis)

    override fun deserialize(
        json: JsonElement,
        typeOf: Type,
        context: JsonDeserializationContext
    ) = DateTime(json.asJsonPrimitive.asLong, DateTimeZone.UTC)
}

enum class Symptom {
    COUGH,
    TEMPERATURE
}
