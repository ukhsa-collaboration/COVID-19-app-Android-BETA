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
import uk.nhs.nhsx.sonar.android.app.onboarding.MainOnboardingActivityTest
import uk.nhs.nhsx.sonar.android.app.onboarding.PermissionActivityTest
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeActivityTest
import uk.nhs.nhsx.sonar.android.app.status.BaseActivityTest
import uk.nhs.nhsx.sonar.android.app.status.StatusActivityTest
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
        val tests = listOf(
            { MainActivityTest(testAppContext).testUnsupportedDevice() },
            { MainActivityTest(testAppContext).testTabletNotSupported() },
            { MainActivityTest(testAppContext).testLaunchWhenOnboardingIsFinishedButNotRegistered() },
            { MainActivityTest(testAppContext).testLaunchWhenStateIsDefault() },
            { MainActivityTest(testAppContext).testLaunchWhenStateIsExposed() },
            { MainActivityTest(testAppContext).testLaunchWhenStateIsSymptomatic() },

            { MainOnboardingActivityTest().testExplanation() },

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

            { DiagnoseReviewActivityTest(testAppContext).testDisplayingYesAnswers() },
            { DiagnoseReviewActivityTest(testAppContext).testDisplayingNoAnswers() },
            { DiagnoseReviewActivityTest(testAppContext).testSubmittingWithoutDate() },
            { DiagnoseReviewActivityTest(testAppContext).testShowingCalendarAndCanceling() },
            { DiagnoseReviewActivityTest(testAppContext).testSelectingTodayFromCalendar() },
            { DiagnoseReviewActivityTest(testAppContext).testSelectingYesterdayFromSpinner() },

            { DiagnoseSubmitActivityTest(testAppContext).testConfirmationIsRequired() },

            { StatusActivityTest(testAppContext).testFeelUnwellCardIsDisplayedWhenInDefaultState() },
            { StatusActivityTest(testAppContext).testFeelUnwellCardIsDisplayedWhenInExposedState() },
            { StatusActivityTest(testAppContext).testFeelUnwellCardIsNotDisplayedWhenInSymptomaticState() },
            { StatusActivityTest(testAppContext).testFeelUnwellCardIsNotDisplayedWhenInPositiveState() },
            { StatusActivityTest(testAppContext).testRegistrationRetry() },
            { StatusActivityTest(testAppContext).testRegistrationPushNotificationNotReceived() },
            { StatusActivityTest(testAppContext).testShowsRecoveryDialogOnResume() },
            { StatusActivityTest(testAppContext).testBottomDialogWhenStateIsExpiredSelectingUpdatingSymptoms() },
            { StatusActivityTest(testAppContext).testBottomDialogWhenStateIsExpiredSelectingNoSymptoms() },
            { StatusActivityTest(testAppContext).testClickOrderTestCardShowsApplyForTest() },
            { StatusActivityTest(testAppContext).testClickOnCurrentAdviceShowsCurrentAdvice() },
            { StatusActivityTest(testAppContext).testStartsViewAndSetsUpCorrectStatusForSymptomaticState() },
            { StatusActivityTest(testAppContext).testStartsViewAndSetsUpCorrectStatusForPositiveTestState() },
            { StatusActivityTest(testAppContext).testBookVirusTestIsNotDisplayedWhenInSymptomaticTestState() },
            { StatusActivityTest(testAppContext).testBookVirusTestIsNotDisplayedWhenInPositiveTestState() },
            { StatusActivityTest(testAppContext).testBookVirusTestIsNotDisplayedWhenInExposedState() },
            { StatusActivityTest(testAppContext).testShowsPositiveTestResultDialogOnResumeForDefaultState() },
            { StatusActivityTest(testAppContext).testShowsNegativeTestResultDialogOnResumeForDefaultState() },
            { StatusActivityTest(testAppContext).testShowsInvalidTestResultDialogOnResumeForDefaultState() },
            { StatusActivityTest(testAppContext).testShowsPositiveTestResultDialogOnResumeForSymptomaticState() },
            { StatusActivityTest(testAppContext).testShowsNegativeTestResultDialogOnResumeForSymptomaticState() },
            { StatusActivityTest(testAppContext).testShowsInvalidTestResultDialogOnResumeForSymptomaticState() },
            { StatusActivityTest(testAppContext).testShowsPositiveTestResultDialogOnResumeForExposedState() },
            { StatusActivityTest(testAppContext).testShowsNegativeTestResultDialogOnResumeForExposedState() },
            { StatusActivityTest(testAppContext).testShowsInvalidTestResultDialogOnResumeForExposedState() },
            { StatusActivityTest(testAppContext).testHideStatusUpdateNotificationWhenNotClicked() },
            { StatusActivityTest(testAppContext).testShowsEnableNotificationOnResume() },
            { StatusActivityTest(testAppContext).testDoesNotEnableAllowNotificationOnResume() },
            { StatusActivityTest(testAppContext).testGrantNotificationPermission() },
            { StatusActivityTest(testAppContext).testShowsUpdateSymptomsDialogWhenPositiveStateExpired() },

            { FlowTest(testAppContext).testRegistration() },
            { FlowTest(testAppContext).testProximityDataUploadOnSymptomaticState() },
            { FlowTest(testAppContext).testQuestionnaireFlowWithNoSymptoms() },
            { FlowTest(testAppContext).testReceivingStatusUpdateNotification() },
            { FlowTest(testAppContext).testExpiredSymptomaticStateRevisitsQuestionnaireAndRemainsToSymptomaticState() },
            { FlowTest(testAppContext).testExpiredSymptomaticStateUpdatingWithNoSymptomsNavigatesToOkActivity() },
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
