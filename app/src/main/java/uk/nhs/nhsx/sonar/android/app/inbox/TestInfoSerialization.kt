/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.inbox

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import uk.nhs.nhsx.sonar.android.app.http.jsonOf

object TestInfoSerialization {

    fun serialize(info: TestInfo): String =
        jsonOf(
            "result" to info.result,
            "date" to info.date.millis
        )

    fun deserialize(json: String?): TestInfo? {
        if (json == null) return null

        return JSONObject(json).let {
            TestInfo(
                result = TestResult.valueOf(it.getString("result")),
                date = DateTime(it.getLong("date"), DateTimeZone.UTC)
            )
        }
    }
}
