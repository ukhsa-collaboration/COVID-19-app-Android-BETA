package uk.nhs.nhsx.sonar.android.app.status.widgets

import android.app.Activity
import android.text.SpannedString
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.interstitials.ApplyForTestActivity
import uk.nhs.nhsx.sonar.android.app.notifications.cancelStatusNotification
import uk.nhs.nhsx.sonar.android.app.status.BottomDialog
import uk.nhs.nhsx.sonar.android.app.status.BottomDialogConfiguration
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.StatusActivity
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import uk.nhs.nhsx.sonar.android.app.util.showExpanded
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat

interface StatusLayout {
    fun refreshStatusLayout(activity: AppCompatActivity)
    fun onResume(activity: StatusActivity) {
    }
}

object StatusLayoutFactory {
    fun from(userState: UserState) =
        when (userState) {
            DefaultState -> DefaultStatusLayout(userState)
            is ExposedState -> ExposedStatusLayout(userState)
            is SymptomaticState -> SymptomaticStatusLayout(userState)
            is PositiveState -> PositiveStatusLayout(userState)
        }
}

class DefaultStatusLayout(val state: UserState) : StatusLayout {

    override fun refreshStatusLayout(activity: AppCompatActivity) {
        createStatusView(
            activity = activity,
            titleRes = R.string.status_initial_title,
            statusColor = StatusView.Color.BLUE
        )
        showNextStepsAdvice(activity, R.string.status_description_01)
        showFeelUnwell(activity)
    }

    override fun onResume(activity: StatusActivity) {
        super.onResume(activity)

        if (activity.userInbox.hasRecovery()) {
            activity.recoveryDialog.showExpanded()
        } else {
            activity.recoveryDialog.dismiss()
        }
    }
}

class ExposedStatusLayout(val state: UserState) : StatusLayout {

    override fun refreshStatusLayout(activity: AppCompatActivity) {
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
        activity.cancelStatusNotification()

        // TODO: refactor this
        val newState = UserStateTransitions.expireExposedState(state)
        activity.userStateStorage.set(newState)

        activity.navigateTo(newState)
    }
}

open class SymptomaticStatusLayout(val state: UserState) : StatusLayout {

    override fun refreshStatusLayout(activity: AppCompatActivity) {
        createStatusView(
            activity = activity,
            titleRes = R.string.status_symptomatic_title,
            statusDescription = createStatusDescriptionForSymptomatic(
                activity = activity,
                userState = state
            ),
            statusColor = StatusView.Color.ORANGE
        )
        showBookTestCard(activity)
    }

    override fun onResume(activity: StatusActivity) {
        if (state.hasExpired()) {
            activity.updateSymptomsDialog.showExpanded()
            activity.checkInReminderNotification.hide()
        } else {
            activity.updateSymptomsDialog.dismiss()
        }
    }
}

class PositiveStatusLayout(val state: UserState) : StatusLayout {

    override fun refreshStatusLayout(activity: AppCompatActivity) {
        createStatusView(
            activity = activity,
            titleRes = R.string.status_positive_test_title,
            statusDescription = createStatusDescriptionForSymptomatic(
                activity = activity,
                userState = state
            ),
            statusColor = StatusView.Color.ORANGE
        )
    }
}

private fun createStatusView(
    activity: AppCompatActivity,
    @StringRes titleRes: Int,
    statusColor: StatusView.Color,
    statusDescription: SpannedString? = null
) {
    val statusView = activity.findViewById<StatusView>(R.id.statusView)
    statusView.setup(
        StatusView.Configuration(
            title = activity.getString(titleRes),
            description = statusDescription,
            statusColor = statusColor
        )
    )
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

fun createStatusDescriptionForSymptomatic(activity: Activity, userState: UserState): SpannedString {
    return buildSpannedString {
        bold {
            append(
                activity.getString(
                    R.string.follow_until_symptomatic_pre,
                    userState.until().toUiFormat()
                )
            )
        }
        append(" ${activity.getString(R.string.follow_until_symptomatic)}")
    }
}

fun createStatusDescriptionForExposed(activity: Activity, userState: UserState): SpannedString {
    return buildSpannedString {
        append(activity.getString(R.string.follow_until))
        bold {
            append("  ${userState.until().toUiFormat()}")
        }
    }
}

private fun showBookTestCard(activity: AppCompatActivity) {
    val view = activity.findViewById<View>(R.id.bookTest)
    view.isVisible = true
    view.setOnClickListener {
        ApplyForTestActivity.start(activity)
    }
}

private fun showFeelUnwell(activity: AppCompatActivity) {
    val view = activity.findViewById<View>(R.id.feelUnwell)
    view.isVisible = true
}

private fun showNextStepsAdvice(activity: AppCompatActivity, @StringRes stringRes: Int) {
    val view = activity.findViewById<TextView>(R.id.nextStepsAdvice)
    view.text = activity.getString(stringRes)
    view.isVisible = true
}

fun toggleNotFeelingCard(activity: AppCompatActivity, enabled: Boolean) {
    val view = activity.findViewById<View>(R.id.feelUnwell)
    view.isClickable = enabled
    view.isEnabled = enabled
}

fun toggleReferenceCodeCard(activity: AppCompatActivity, enabled: Boolean) {
    val view = activity.findViewById<View>(R.id.reference_link_card)
    view.isClickable = enabled
    view.isEnabled = enabled
}

fun handleTestResult(userInbox: UserInbox, testResultDialog: BottomDialog) {
    if (userInbox.hasTestInfo()) {
        val info = userInbox.getTestInfo()
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
