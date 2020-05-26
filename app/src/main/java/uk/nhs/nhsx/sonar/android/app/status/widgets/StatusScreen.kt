package uk.nhs.nhsx.sonar.android.app.status.widgets

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.CheckinState
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.tests.ApplyForTestActivity
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

private fun createStatusView(activity: AppCompatActivity, userState: UserState, @StringRes titleRes: Int) {
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

private fun createBootTestCard(activity: AppCompatActivity) {
    val view = activity.findViewById<View>(R.id.book_test_card)
    view.isVisible = true
    view.setOnClickListener {
        ApplyForTestActivity.start(activity)
    }
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
        createBootTestCard(activity)
    }
}

// TODO: do we need CheckIn State?
class CheckInStatusScreen(val state: UserState) : StatusScreen {

    override fun setStatusScreen(activity: AppCompatActivity) {
        createStatusView(activity, state, R.string.status_symptomatic_title)
        createBootTestCard(activity)
    }
}

class DummyScreen(val state: UserState) : StatusScreen {
    override fun setStatusScreen(activity: AppCompatActivity) {
        Timber.d("DummyScreen: should implement screen for state: ${state::class.java.simpleName}?")
    }
}
