/*
 * Copyright © 2020 NHSX. All rights reserved.
 */
package uk.nhs.nhsx.sonar.android.app.referencecode

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.ReferenceCodeRobot

class ReferenceCodeActivityTest : EspressoTest() {

    private val referenceCodeRobot = ReferenceCodeRobot()

    @Test
    fun showsReferenceCode() {
        startActivityWithState<ReferenceCodeActivity>()
        referenceCodeRobot.checkReferenceCodeIs("REF CODE #202")
    }
}
