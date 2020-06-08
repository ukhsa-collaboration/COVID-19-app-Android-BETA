package uk.nhs.nhsx.sonar.android.app.status.widgets

import android.app.Activity
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.interstitials.ApplyForTestActivity
import uk.nhs.nhsx.sonar.android.app.status.BottomDialog
import uk.nhs.nhsx.sonar.android.app.status.BottomDialogConfiguration
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.ExposedSymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.StatusActivity
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.util.showExpanded
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat

abstract class StatusLayout {

    fun refreshStatusLayout(activity: StatusActivity) {
        hideNotSharedWidgets(activity)
        refreshLayout(activity)
    }

    abstract fun onResume(activity: StatusActivity)

    protected abstract fun refreshLayout(activity: StatusActivity)

    private fun hideNotSharedWidgets(activity: StatusActivity) {
        activity.findViewById<View>(R.id.bookTest).isVisible = false
        activity.findViewById<View>(R.id.feelUnwell).isVisible = false
        activity.findViewById<View>(R.id.nextStepsAdvice).isVisible = false
    }
}

object StatusLayoutFactory {
    fun from(userState: UserState) =
        when (userState) {
            is DefaultState -> DefaultStatusLayout(userState)
            is ExposedState -> ExposedStatusLayout(userState)
            is SymptomaticState -> SymptomaticStatusLayout(userState)
            is ExposedSymptomaticState -> SymptomaticStatusLayout(userState)
            is PositiveState -> PositiveStatusLayout(userState)
        }
}

class DefaultStatusLayout(val state: DefaultState) : StatusLayout() {

    override fun refreshLayout(activity: StatusActivity) {
        createStatusView(
            activity = activity,
            titleRes = R.string.status_initial_title,
            statusColor = StatusView.Color.BLUE
        )
        showNextStepsAdvice(activity, R.string.status_description_01)
        showFeelUnwell(activity)
    }

    override fun onResume(activity: StatusActivity) {
        if (activity.userInbox.hasRecovery()) {
            activity.recoveryDialog.showExpanded()
        } else {
            activity.recoveryDialog.dismiss()
        }
        handleTestResult(activity, activity.testResultDialog)
    }
}

class ExposedStatusLayout(val state: ExposedState) : StatusLayout() {

    override fun refreshLayout(activity: StatusActivity) {
        createStatusView(
            activity = activity,
            titleRes = R.string.status_exposed_title,
            statusDescription = createStatusDescriptionForExposed(
                activity = activity,
                userState = state
            ),
            statusColor = StatusView.Color.ORANGE
        )
        showNextStepsAdvice(activity, R.string.symptomatic_state_advice_info)
        showFeelUnwell(activity)
    }

    override fun onResume(activity: StatusActivity) {
        activity.exposedNotification.hide()

        activity.userStateMachine.transitionOnExpiredExposedState()
        activity.refreshState()

        handleTestResult(activity, activity.testResultDialog)
    }
}

class SymptomaticStatusLayout(val state: UserState) : StatusLayout() {

    override fun refreshLayout(activity: StatusActivity) {
        createStatusView(
            activity = activity,
            titleRes = R.string.status_symptomatic_title,
            statusDescription = createStatusDescriptionForSymptomaticAndPositive(
                activity = activity,
                userState = state
            ),
            statusColor = StatusView.Color.ORANGE
        )
        showBookTestCard(activity)
    }

    override fun onResume(activity: StatusActivity) {
        val userTestedNegative =
            activity.userInbox.hasTestInfo() && activity.userInbox.getTestInfo().result == TestResult.NEGATIVE

        if (userTestedNegative && state.hasExpired()) {
            displaySpecialCheckinDialog(activity)
        } else {
            displayNormalCheckinDialogIfExpired(activity, state)
            handleTestResult(activity, activity.testResultDialog)
        }
    }
}

class PositiveStatusLayout(val state: PositiveState) : StatusLayout() {

    override fun refreshLayout(activity: StatusActivity) {
        createStatusView(
            activity = activity,
            titleRes = R.string.status_positive_test_title,
            statusDescription = createStatusDescriptionForSymptomaticAndPositive(
                activity = activity,
                userState = state
            ),
            statusColor = StatusView.Color.ORANGE
        )
    }

    override fun onResume(activity: StatusActivity) {
        displayNormalCheckinDialogIfExpired(activity, state)
        handleTestResult(activity, activity.testResultDialog)
    }
}

private fun createStatusView(
    activity: StatusActivity,
    @StringRes titleRes: Int,
    statusColor: StatusView.Color,
    statusDescription: String? = null
) {
    val statusView = activity.findViewById<StatusView>(R.id.statusView)
    statusView.setup(
        StatusView.Configuration(
            title = activity.getString(titleRes),
            description = statusDescription,
            statusColor = statusColor
        )
    )
    activity.setTitle(titleRes)
}

fun createTestResultDialog(activity: Activity, userInbox: UserInbox): BottomDialog {
    val configuration = BottomDialogConfiguration(
        titleResId = R.string.negative_test_result_title,
        textResId = R.string.negative_test_result_description,
        secondCtaResId = R.string.close,
        isHideable = false
    )
    return BottomDialog(activity, configuration,
        onCancel = {
            userInbox.dismissTestInfo()
            activity.finish()
        },
        onSecondCtaClick = {
            userInbox.dismissTestInfo()
        }
    )
}

fun createStatusDescriptionForSymptomaticAndPositive(
    activity: StatusActivity,
    userState: UserState
): String {
    return activity.getString(
        R.string.follow_until_symptomatic,
        userState.until().toUiFormat()
    )
}

fun createStatusDescriptionForExposed(activity: StatusActivity, userState: UserState): String {
    return activity.getString(
        R.string.follow_until_exposed,
        userState.until().toUiFormat()
    )
}

private fun showBookTestCard(activity: StatusActivity) {
    val view = activity.findViewById<View>(R.id.bookTest)
    view.isVisible = true
    view.setOnClickListener {
        ApplyForTestActivity.start(activity)
    }
}

private fun showFeelUnwell(activity: StatusActivity) {
    val view = activity.findViewById<View>(R.id.feelUnwell)
    view.isVisible = true
}

private fun showNextStepsAdvice(activity: StatusActivity, @StringRes stringRes: Int) {
    val view = activity.findViewById<TextView>(R.id.nextStepsAdvice)
    view.text = activity.getString(stringRes)
    view.isVisible = true
}

fun toggleNotFeelingCard(activity: StatusActivity, enabled: Boolean) {
    val view = activity.findViewById<View>(R.id.feelUnwell)
    view.isClickable = enabled
    view.isEnabled = enabled
}

fun toggleReferenceCodeCard(activity: StatusActivity, enabled: Boolean) {
    val view = activity.findViewById<View>(R.id.reference_link_card)
    view.isClickable = enabled
    view.isEnabled = enabled
}

fun handleTestResult(activity: StatusActivity, testResultDialog: BottomDialog) {
    if (activity.userInbox.hasTestInfo()) {
        activity.testResultNotification.hide()

        val info = activity.userInbox.getTestInfo()
        when (info.result) {
            TestResult.POSITIVE -> {
                testResultDialog.setTitleResId(R.string.positive_test_result_title)
                testResultDialog.setTextResId(R.string.positive_test_result_description)
            }
            TestResult.NEGATIVE -> {
                testResultDialog.setTitleResId(R.string.negative_test_result_title)
                testResultDialog.setTextResId(R.string.negative_test_result_description)
            }
            TestResult.INVALID -> {
                testResultDialog.setTitleResId(R.string.invalid_test_result_title)
                testResultDialog.setTextResId(R.string.invalid_test_result_description)
            }
        }
        testResultDialog.showExpanded()
    } else {
        testResultDialog.dismiss()
    }
}

private fun displaySpecialCheckinDialog(activity: StatusActivity) {
    activity.checkInReminderNotification.hide()
    activity.negativeResultCheckinReminderDialog.showExpanded()
}

private fun displayNormalCheckinDialogIfExpired(activity: StatusActivity, state: UserState) {
    if (state.hasExpired()) {
        activity.checkInReminderNotification.hide()
        activity.checkinReminderDialog.showExpanded()
    } else {
        activity.checkinReminderDialog.dismiss()
    }
}
