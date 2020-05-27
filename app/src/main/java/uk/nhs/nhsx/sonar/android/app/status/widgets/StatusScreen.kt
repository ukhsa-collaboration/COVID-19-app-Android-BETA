package uk.nhs.nhsx.sonar.android.app.status.widgets

import android.app.Activity
import android.content.Context
import android.text.SpannedString
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.interstitials.ApplyForTestActivity
import uk.nhs.nhsx.sonar.android.app.notifications.cancelStatusNotification
import uk.nhs.nhsx.sonar.android.app.status.BottomDialog
import uk.nhs.nhsx.sonar.android.app.status.BottomDialogConfiguration
import uk.nhs.nhsx.sonar.android.app.status.CheckinState
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

interface StatusScreen {
    fun setStatusScreen(activity: AppCompatActivity)
    fun onResume(activity: StatusActivity) {
    }
}

object StatusScreenFactory {
    fun from(userState: UserState) =
        when (userState) {
            DefaultState -> DummyScreen(userState)
            is ExposedState -> ExposedStatusScreen(userState)
            is SymptomaticState -> SymptomaticStatusScreen(userState)
            is CheckinState -> CheckInStatusScreen(userState)
            is PositiveState -> PositiveStatusScreen(userState)
        }
}

private fun createStatusView(
    activity: AppCompatActivity,
    @StringRes titleRes: Int,
    statusDescription: SpannedString
) {
    val statusView = activity.findViewById<StatusView>(R.id.statusView)
    statusView.setup(
        StatusView.Configuration(
            title = activity.getString(titleRes),
            description = statusDescription,
            statusColor = StatusView.Color.ORANGE
        )
    )
}

private fun createBookTestCard(activity: AppCompatActivity) {
    val view = activity.findViewById<View>(R.id.bookTest)
    view.isVisible = true
    view.setOnClickListener {
        ApplyForTestActivity.start(activity)
    }
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
        })
}

class PositiveStatusScreen(val state: UserState) : StatusScreen {

    override fun setStatusScreen(activity: AppCompatActivity) {
        val description = createStatusDescriptionForSymptomatic(activity, state)
        createStatusView(activity, R.string.status_positive_test_title, description)
        activity.findViewById<View>(R.id.bookTest).isVisible = false
    }
}

open class SymptomaticStatusScreen(val state: UserState) : StatusScreen {

    override fun setStatusScreen(activity: AppCompatActivity) {
        val description = createStatusDescriptionForSymptomatic(activity, state)
        createStatusView(activity, R.string.status_symptomatic_title, description)
        createBookTestCard(activity)
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

// TODO: do we need CheckIn State?
class CheckInStatusScreen(state: UserState) : SymptomaticStatusScreen(state)

class DummyScreen(val state: UserState) : StatusScreen {
    override fun setStatusScreen(activity: AppCompatActivity) {
        Timber.d("DummyScreen: should implement screen for state: ${state::class.java.simpleName}?")
    }
}

class ExposedStatusScreen(val state: UserState) : StatusScreen {
    override fun setStatusScreen(activity: AppCompatActivity) {
        val description = createStatusDescriptionForExposed(activity, state)
        createStatusView(activity, R.string.status_exposed_title, description)
    }

    override fun onResume(activity: StatusActivity) {
        activity.cancelStatusNotification()

        // TODO: refactor this
        val newState = UserStateTransitions.expireExposedState(state)
        activity.userStateStorage.set(newState)

        activity.navigateTo(newState)
    }
}

fun createStatusDescriptionForSymptomatic(context: Context, userState: UserState): SpannedString {
    return buildSpannedString {
        bold {
            append(
                context.getString(
                    R.string.follow_until_symptomatic_pre,
                    userState.until().toUiFormat()
                )
            )
        }
        append(" ${context.getString(R.string.follow_until_symptomatic)}")
    }
}

fun createStatusDescriptionForExposed(context: Context, userState: UserState): SpannedString {
    return buildSpannedString {
        append(context.getString(R.string.follow_until))
        bold {
            append("  ${userState.until().toUiFormat()}")
        }
    }
}
