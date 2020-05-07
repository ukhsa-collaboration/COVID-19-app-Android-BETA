/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import io.mockk.Called
import io.mockk.mockk
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics
import uk.nhs.nhsx.sonar.android.app.analytics.partialPostcodeProvided

class PostCodeValidatorTest {

    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val analytics = mockk<SonarAnalytics>(relaxed = true)
    private val validator = PostCodeValidator(postCodeProvider, analytics)

    @Test
    fun emptyPostCode() {
        val isValid = validator.validate("")

        assertThat(isValid).isFalse()

        verifyAll {
            postCodeProvider wasNot Called
            analytics wasNot Called
        }
    }

    @Test
    fun invalidPostCode() {
        val isValid = validator.validate("A")

        assertThat(isValid).isFalse()

        verifyAll {
            postCodeProvider wasNot Called
            analytics wasNot Called
        }
    }

    @Test
    fun validPostCodePrefix() {
        val isValid = validator.validate("SW15")

        assertThat(isValid).isTrue()

        verifyAll {
            postCodeProvider.set("SW15")
            analytics.trackEvent(partialPostcodeProvided())
        }
    }
}
