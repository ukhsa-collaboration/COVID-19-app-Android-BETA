package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

fun buildExposedState(
    since: DateTime = DateTime.now().plusDays(1),
    until: DateTime = DateTime.now().plusDays(1)
) = ExposedState(since, until)

fun buildSymptomaticState(
    since: DateTime = DateTime.now().plusDays(1),
    until: DateTime = DateTime.now().plusDays(1),
    symptoms: NonEmptySet<Symptom> = nonEmptySetOf(Symptom.COUGH)
) = SymptomaticState(since, until, symptoms)

fun buildPositiveState(
    since: DateTime = DateTime.now().plusDays(1),
    until: DateTime = DateTime.now().plusDays(1),
    symptoms: NonEmptySet<Symptom> = nonEmptySetOf(Symptom.COUGH)
) = PositiveState(since, until, symptoms)

fun buildCheckinState(
    until: DateTime = DateTime.now().plusDays(1),
    symptoms: NonEmptySet<Symptom> = nonEmptySetOf(Symptom.COUGH)
) = CheckinState(until, until, symptoms)
