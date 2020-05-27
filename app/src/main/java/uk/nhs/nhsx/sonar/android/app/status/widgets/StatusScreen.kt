package uk.nhs.nhsx.sonar.android.app.status.widgets

import android.app.Activity
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
import uk.nhs.nhsx.sonar.android.app.status.BottomDialog
import uk.nhs.nhsx.sonar.android.app.status.BottomDialogConfiguration
import uk.nhs.nhsx.sonar.android.app.status.CheckinState
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.util.showExpanded
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat

interface StatusScreen {
    fun setStatusScreen(activity: AppCompatActivity)
}

object StatusScreenFactory {
    fun from(userState: UserState) =
        when (userState) {
            DefaultState -> DummyScreen(userState)
            is ExposedState -> DummyScreen(userState)
            is SymptomaticState -> SymptomaticStatusScreen(userState)
            is CheckinState -> CheckInStatusScreen(userState)
            is PositiveState -> PositiveStatusScreen(userState)
        }
}

private fun createStatusView(
    activity: AppCompatActivity,
    userState: UserState,
    @StringRes titleRes: Int
) {
    val statusView = activity.findViewById<StatusView>(R.id.statusView)
    val statusDescription = buildSpannedString {
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
    statusView.setup(
        StatusView.Configuration(
            title = activity.getString(titleRes),
            description = statusDescription,
            statusColor = StatusView.Color.ORANGE
        )
    )
}

private fun createBookTestCard(activity: AppCompatActivity) {
    val view = activity.findViewById<View>(R.id.book_test_card)
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
        createStatusView(activity, state, R.string.status_positive_test_title)
        activity.findViewById<View>(R.id.book_test_card).isVisible = false
    }
}

class SymptomaticStatusScreen(val state: UserState) : StatusScreen {

    override fun setStatusScreen(activity: AppCompatActivity) {
        createStatusView(activity, state, R.string.status_symptomatic_title)
        createBookTestCard(activity)
    }
}

// TODO: do we need CheckIn State?
class CheckInStatusScreen(val state: UserState) : StatusScreen {

    override fun setStatusScreen(activity: AppCompatActivity) {
        createStatusView(activity, state, R.string.status_symptomatic_title)
        createBookTestCard(activity)
    }
}

class DummyScreen(val state: UserState) : StatusScreen {
    override fun setStatusScreen(activity: AppCompatActivity) {
        Timber.d("DummyScreen: should implement screen for state: ${state::class.java.simpleName}?")
    }
}
