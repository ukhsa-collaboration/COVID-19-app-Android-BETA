/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.persistence

const val ID_NOT_REGISTERED = "00000000-0000-0000-0000-000000000000"

interface SonarIdProvider {
    fun getSonarId(): String
    fun hasProperSonarId(): Boolean
    fun setSonarId(sonarId: String)
}
