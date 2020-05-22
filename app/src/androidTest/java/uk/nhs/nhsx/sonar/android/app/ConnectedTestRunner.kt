/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseReviewActivityTest
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseSubmitActivityTest
import uk.nhs.nhsx.sonar.android.app.onboarding.PermissionActivityTest
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeActivityTest
import uk.nhs.nhsx.sonar.android.app.status.AtRiskActivityTest
import uk.nhs.nhsx.sonar.android.app.status.BaseActivityTest
import uk.nhs.nhsx.sonar.android.app.status.IsolateActivityTest
import uk.nhs.nhsx.sonar.android.app.status.OkActivityTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper

@RunWith(AndroidJUnit4::class)
class ConnectedTestRunner {

    @get:Rule
    val activityRule: ActivityTestRule<FlowTestStartActivity> =
        ActivityTestRule(FlowTestStartActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*AndroidLocationHelper.requiredLocationPermissions)

    lateinit var testAppContext: TestApplicationContext

    @Before
    fun setup() {
        testAppContext = TestApplicationContext(activityRule)
    }

    private fun resetApp() {
        testAppContext.reset()
        testAppContext.app.startTestActivity<FlowTestStartActivity>()
    }

    @After
    fun teardown() {
        testAppContext.shutdownMockServer()
    }

    @Test
    fun runAllTests() {
        val tests = listOf<() -> Unit>(
            { MainActivityTest(testAppContext).testExplanation() },
            { MainActivityTest(testAppContext).testUnsupportedDevice() },
            { MainActivityTest(testAppContext).testTabletNotSupported() },
            { MainActivityTest(testAppContext).testLaunchWhenOnboardingIsFinishedButNotRegistered() },
            { MainActivityTest(testAppContext).testLaunchWhenStateIsDefault() },
            { MainActivityTest(testAppContext).testLaunchWhenStateIsAmber() },
            { MainActivityTest(testAppContext).testLaunchWhenStateIsRed() },

            { PostCodeActivityTest(testAppContext).pristineState() },
            { PostCodeActivityTest(testAppContext).emptyPostCodeShowsInvalidHint() },
            { PostCodeActivityTest(testAppContext).invalidPostCodeShowsInvalidHint() },
            { PostCodeActivityTest(testAppContext).validPostCodeProceedsToNextView() },

            { PermissionActivityTest(testAppContext).testUnsupportedDevice() },
            { PermissionActivityTest(testAppContext).testEnableBluetooth() },
            { PermissionActivityTest(testAppContext).testGrantLocationPermission() },
            { PermissionActivityTest(testAppContext).testEnableLocationAccess() },

            { BaseActivityTest(testAppContext).testResumeWhenBluetoothIsDisabled() },
            { BaseActivityTest(testAppContext).testResumeWhenLocationAccessIsDisabled() },
            { BaseActivityTest(testAppContext).testResumeWhenLocationPermissionIsRevoked() },

            { OkActivityTest(testAppContext).testRegistrationRetry() },
            { OkActivityTest(testAppContext).testRegistrationPushNotificationNotReceived() },
            { OkActivityTest(testAppContext).testShowsRecoveryDialogOnResume() },

            { AtRiskActivityTest(testAppContext).testHideStatusUpdateNotificationWhenNotClicked() },

            { DiagnoseReviewActivityTest(testAppContext).testDisplayingYesAnswers() },
            { DiagnoseReviewActivityTest(testAppContext).testDisplayingNoAnswers() },
            { DiagnoseReviewActivityTest(testAppContext).testSubmittingWithoutDate() },
            { DiagnoseReviewActivityTest(testAppContext).testShowingCalendarAndCanceling() },
            { DiagnoseReviewActivityTest(testAppContext).testSelectingTodayFromCalendar() },
            { DiagnoseReviewActivityTest(testAppContext).testSelectingYesterdayFromSpinner() },

            { DiagnoseSubmitActivityTest(testAppContext).testConfirmationIsRequired() },

            { IsolateActivityTest(testAppContext).testWhenStateIsExpired() },
            { IsolateActivityTest(testAppContext).testClickOrderTestCardShowsApplyForTest() },

            { FlowTest(testAppContext).testRegistration() },
            { FlowTest(testAppContext).testProximityDataUploadOnSymptomaticState() },
            { FlowTest(testAppContext).testQuestionnaireFlowWithNoSymptoms() },
            { FlowTest(testAppContext).testReceivingStatusUpdateNotification() },
            { FlowTest(testAppContext).testExpiredRedStateRevisitsQuestionnaireAndRemainsToRedState() },
            { FlowTest(testAppContext).testEnableBluetoothThroughNotification() }
        )

        tests.forEach {
            resetApp()
            it()
        }
    }
}

inline fun <reified T : Activity> Context.startTestActivity(config: Intent.() -> Unit = {}) {
    val intent = Intent(this, T::class.java)
        .apply { addFlags(FLAG_ACTIVITY_NEW_TASK) }
        .apply(config)

    InstrumentationRegistry
        .getInstrumentation()
        .startActivitySync(intent)
}
