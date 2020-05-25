package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.BottomDialogRobot
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class IsolateActivityTest(private val testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val isolateRobot = IsolateRobot()
    private val applyForTestRobot = ApplyForTestRobot()
    private val bottomDialogRobot = BottomDialogRobot()
    private val expiredSymptomaticState =
        SymptomaticState(
            DateTime.now(UTC).minusSeconds(1),
            DateTime.now(UTC).minusSeconds(1),
            nonEmptySetOf(TEMPERATURE)
        )

    private fun startActivity(state: SymptomaticState) {
        testAppContext.setFullValidUser(state)

        app.startTestActivity<IsolateActivity>()
    }

    fun testBottomDialogWhenStateIsExpiredSelectingUpdatingSymptoms() {
        startActivity(expiredSymptomaticState)

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickFirstCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }

    fun testBottomDialogWhenStateIsExpiredSelectingNoSymptoms() {
        startActivity(expiredSymptomaticState)

        bottomDialogRobot.checkUpdateSymptomsDialogIsDisplayed()
        bottomDialogRobot.clickSecondCtaButton()
        bottomDialogRobot.checkBottomDialogIsNotDisplayed()
    }

    fun testClickOrderTestCardShowsApplyForTest() {
        val date = DateTime.now(UTC).plusDays(1)
        val symptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        startActivity(symptomaticState)

        isolateRobot.clickBookTestCard()
        applyForTestRobot.checkActivityIsDisplayed()
    }
}
