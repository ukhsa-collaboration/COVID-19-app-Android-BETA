package uk.nhs.nhsx.sonar.android.app.diagnose

import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext

class DiagnoseSubmitActivityTest(testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val diagnoseSubmitRobot = DiagnoseSubmitRobot()

    private fun startActivity() {
        app.startTestActivity<DiagnoseSubmitActivity> {
            putSymptoms(setOf(COUGH, TEMPERATURE))
            putExtra("SYMPTOMS_DATE", DateTime.now().millis)
        }
    }

    fun testConfirmationIsRequired() {
        startActivity()

        diagnoseSubmitRobot.submit()
        diagnoseSubmitRobot.checkConfirmationIsNeeded()
    }
}
