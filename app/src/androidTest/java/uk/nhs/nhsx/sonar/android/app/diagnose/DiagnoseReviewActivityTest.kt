package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.diagnose.review.DiagnoseReviewActivity
import uk.nhs.nhsx.sonar.android.app.startTestActivity
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.Symptom.values
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext

class DiagnoseReviewActivityTest(testAppContext: TestApplicationContext) {

    private val app = testAppContext.app
    private val diagnoseReviewRobot = DiagnoseReviewRobot()

    private fun startActivity(symptoms: Set<Symptom>) {
        app.startTestActivity<DiagnoseReviewActivity> {
            putSymptoms(symptoms)
        }
    }

    private val allSymptoms = values().toSet()
    private val someSymptoms = setOf(COUGH, TEMPERATURE)
    private val noSymptoms = emptySet<Symptom>()

    fun testDisplayingYesAnswers() {
        startActivity(allSymptoms)

        onView(withId(R.id.review_answer_temperature)).check(matches(withText(R.string.i_do_temperature)))
        onView(withId(R.id.review_answer_cough)).check(matches(withText(R.string.i_do_cough)))
        onView(withId(R.id.review_answer_anosmia)).check(matches(withText(R.string.i_do_anosmia)))
        onView(withId(R.id.review_answer_sneeze)).check(matches(withText(R.string.i_do_sneeze)))
        onView(withId(R.id.review_answer_stomach)).check(matches(withText(R.string.i_do_stomach)))
    }

    fun testDisplayingNoAnswers() {
        startActivity(noSymptoms)

        onView(withId(R.id.review_answer_temperature)).check(matches(withText(R.string.i_do_not_temperature)))
        onView(withId(R.id.review_answer_cough)).check(matches(withText(R.string.i_do_not_cough)))
        onView(withId(R.id.review_answer_anosmia)).check(matches(withText(R.string.i_do_not_anosmia)))
        onView(withId(R.id.review_answer_sneeze)).check(matches(withText(R.string.i_do_not_sneeze)))
        onView(withId(R.id.review_answer_stomach)).check(matches(withText(R.string.i_do_not_stomach)))
    }

    fun testSubmittingWithoutDate() {
        startActivity(someSymptoms)

        diagnoseReviewRobot.checkNoDateSelected()
        diagnoseReviewRobot.submit()
        diagnoseReviewRobot.checkDateErrorIsDisplayed()
    }

    fun testShowingCalendarAndCanceling() {
        startActivity(someSymptoms)

        diagnoseReviewRobot.openCalendar()
        diagnoseReviewRobot.cancelCalendar()
        diagnoseReviewRobot.checkNoDateSelected()
    }

    fun testSelectingTodayFromCalendar() {
        startActivity(someSymptoms)

        diagnoseReviewRobot.openCalendar()
        diagnoseReviewRobot.okCalendar()
        diagnoseReviewRobot.checkSelectedDate(LocalDate.now())
    }

    fun testSelectingYesterdayFromSpinner() {
        startActivity(someSymptoms)

        diagnoseReviewRobot.selectYesterday()
        diagnoseReviewRobot.checkSelectedDate(R.string.yesterday)
    }
}
