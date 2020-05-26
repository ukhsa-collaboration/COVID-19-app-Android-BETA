package uk.nhs.nhsx.sonar.android.app.status.widgets

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.CheckinState
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat

interface StatusScreen {
    fun setStatusScreen(activity: AppCompatActivity)
}

object StatusScreenFactory {
    fun from(userState: UserState) =
        when (userState) {
            DefaultState -> TODO()
            is ExposedState -> TODO()
            is SymptomaticState -> SymptomaticStatusScreen(userState)
            is CheckinState -> CheckInStatusScreen(userState)
            is PositiveState -> PositiveStatusScreen(userState)
        }
}

fun createStatusView(activity: AppCompatActivity, userState: UserState, @StringRes titleRes: Int) {
    val statusView = activity.findViewById<StatusView>(R.id.statusView)
    val statusDescription = buildSpannedString {
        bold {
            append(activity.getString(R.string.follow_until_symptomatic_pre, userState.until().toUiFormat()))
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

class PositiveStatusScreen(val state: UserState) : StatusScreen {

    override fun setStatusScreen(activity: AppCompatActivity) {
        setStatusView(activity)
    }

    private fun setStatusView(activity: AppCompatActivity) {
        createStatusView(activity, state, R.string.status_positive_test_title)
    }
}

class SymptomaticStatusScreen(val state: UserState) : StatusScreen {

    override fun setStatusScreen(activity: AppCompatActivity) {
        setStatusView(activity)
    }

    private fun setStatusView(activity: AppCompatActivity) {
        createStatusView(activity, state, R.string.status_symptomatic_title)
    }
}

// TODO: do we need CheckIn State?
class CheckInStatusScreen(val state: UserState) : StatusScreen {

    override fun setStatusScreen(activity: AppCompatActivity) {
        setStatusView(activity)
    }

    private fun setStatusView(activity: AppCompatActivity) {
        createStatusView(activity, state, R.string.status_symptomatic_title)
    }
}
