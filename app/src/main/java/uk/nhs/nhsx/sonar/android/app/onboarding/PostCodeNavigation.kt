/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

sealed class PostCodeNavigation {
    object Permissions : PostCodeNavigation()
}
