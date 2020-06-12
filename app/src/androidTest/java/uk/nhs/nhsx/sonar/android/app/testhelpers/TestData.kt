package uk.nhs.nhsx.sonar.android.app.testhelpers

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class TestData {

    val symptomaticState = SymptomaticState(
        DateTime.now(DateTimeZone.UTC).minusDays(1),
        DateTime.now(DateTimeZone.UTC).plusDays(1),
        nonEmptySetOf(Symptom.TEMPERATURE)
    )
}
