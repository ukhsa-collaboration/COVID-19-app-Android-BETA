package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class IsolateActivityTest(private val testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val isolateRobot = IsolateRobot()
    private val applyForTestRobot = ApplyForTestRobot()

    private fun startActivity(state: RedState) {
        testAppContext.setFullValidUser(state)

        app.startTestActivity<IsolateActivity>()
    }

    fun testWhenStateIsExpired() {
        val expiredRedState = RedState(DateTime.now(UTC).minusSeconds(1), nonEmptySetOf(TEMPERATURE))

        startActivity(expiredRedState)

        isolateRobot.checkPopUpIsDisplayed()
    }

    fun testClickOrderTestCardShowsApplyForTest() {
        val redState = RedState(DateTime.now(UTC).plusDays(1), nonEmptySetOf(TEMPERATURE))

        startActivity(redState)

        isolateRobot.clickBookTestCard()
        applyForTestRobot.checkActivityIsDisplayed()
    }
}
