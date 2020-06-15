/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCodeValidator @Inject constructor(private val postCodeProvider: PostCodeProvider) {

    private val postCodeRegex = Regex("^[A-Z]{1,2}[0-9R][0-9A-Z]?")

    fun validate(postCode: String): Boolean {
        val postCodeUpperCased = postCode.toUpperCase(Locale.UK)
        val isValid = postCodeRegex.matches(postCodeUpperCased)

        if (isValid) {
            postCodeProvider.set(postCodeUpperCased)
        }

        return isValid
    }
}
