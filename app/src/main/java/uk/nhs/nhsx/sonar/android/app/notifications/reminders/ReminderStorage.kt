/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications.reminders

import android.content.Context
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.app.http.jsonOf
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceSerializingProvider
import javax.inject.Inject

class ReminderStorage @Inject constructor(context: Context) :
    SharedPreferenceSerializingProvider<Reminder>(
        context,
        preferenceName = "reminder_notification_storage",
        preferenceKey = "reminder_notification",
        serialize = ::serializer,
        deserialize = ::deserialize
    ) {

    fun setPendingReminder(reminder: Reminder) = this.set(reminder)

    fun getPendingReminder(): Reminder? =
        get().let {
            if (it != NO_VALUE) it else null
        }

    override fun get(): Reminder =
        when (val value = sharedPreferences.all[preferenceKey]) {
            is Long -> Reminder(
                time = value,
                type = Reminder.REMINDER_TYPE_CHECK_IN
            )
            else -> (value as String?).let(Companion::deserialize)
        }

    companion object {
        private val NO_VALUE = Reminder(-1L, -1)

        fun deserialize(value: String?): Reminder {
            if (value == null)
                return NO_VALUE

            val jsonObj = JSONObject(value)

            return Reminder(
                time = jsonObj.getLong("time"),
                type = jsonObj.getInt("type")
            )
        }

        fun serializer(reminder: Reminder): String =
            jsonOf(
                "time" to reminder.time,
                "type" to reminder.type
            )
    }
}
