/*
 * Copyright © 2020 NHSX. All rights reserved.
 */
package uk.nhs.nhsx.sonar.android.app.referencecode

import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext

class ReferenceCodeActivityTest(private val testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val referenceCodeRobot = ReferenceCodeRobot()

    fun testShowsReferenceCode() {
        testAppContext.setFullValidUser()
        app.startTestActivity<ReferenceCodeActivity>()
        referenceCodeRobot.checkReferenceCodeIs("REF CODE #202")
    }
}
