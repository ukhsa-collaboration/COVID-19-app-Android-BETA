/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Intent
import uk.nhs.nhsx.sonar.android.app.status.Symptom

private const val SYMPTOMS = "SYMPTOMS"

fun Intent.putSymptoms(symptoms: Set<Symptom>) {
    putExtra(SYMPTOMS, symptoms.map { it.value }.toTypedArray())
}

fun Intent.getSymptoms(): Set<Symptom> {
    return (getStringArrayExtra(SYMPTOMS) ?: emptyArray())
        .mapNotNull { Symptom.fromValue(it) }
        .toSet()
}
