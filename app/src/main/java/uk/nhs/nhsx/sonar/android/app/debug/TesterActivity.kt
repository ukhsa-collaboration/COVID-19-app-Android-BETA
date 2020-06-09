/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.debug

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.DatePicker
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_test.app_version
import kotlinx.android.synthetic.main.activity_test.continueButton
import kotlinx.android.synthetic.main.activity_test.encrypted_broadcast_id
import kotlinx.android.synthetic.main.activity_test.events
import kotlinx.android.synthetic.main.activity_test.exportButton
import kotlinx.android.synthetic.main.activity_test.firebase_token
import kotlinx.android.synthetic.main.activity_test.no_events
import kotlinx.android.synthetic.main.activity_test.resetButton
import kotlinx.android.synthetic.main.activity_test.setDefaultState
import kotlinx.android.synthetic.main.activity_test.setExposedNotification
import kotlinx.android.synthetic.main.activity_test.setExposedState
import kotlinx.android.synthetic.main.activity_test.setExposedSymptomaticState
import kotlinx.android.synthetic.main.activity_test.setPositiveState
import kotlinx.android.synthetic.main.activity_test.setSymptomaticState
import kotlinx.android.synthetic.main.activity_test.setTestInvalidNotification
import kotlinx.android.synthetic.main.activity_test.setTestNegativeNotification
import kotlinx.android.synthetic.main.activity_test.setTestPositiveNotification
import kotlinx.android.synthetic.main.activity_test.showCurrentState
import kotlinx.android.synthetic.main.activity_test.sonar_id
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.common.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramProvider
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramStorage
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationHandler
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.ExposedSymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserStateMachine
import uk.nhs.nhsx.sonar.android.app.util.appVersion
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import uk.nhs.nhsx.sonar.android.app.util.observe
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import uk.nhs.nhsx.sonar.android.app.util.toUtcNormalized
import javax.inject.Inject

fun cryptogramColourAndInverse(cryptogramBytes: ByteArray): Pair<Int, Int> {
    val r = cryptogramBytes[0].toInt()
    val g = cryptogramBytes[1].toInt()
    val b = cryptogramBytes[2].toInt()
    return Pair(Color.rgb(r, g, b), Color.rgb(255 - r, 255 - g, 255 - b))
}

class TesterActivity : AppCompatActivity(R.layout.activity_test) {

    @Inject
    lateinit var userStateMachine: UserStateMachine

    @Inject
    lateinit var userInbox: UserInbox

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var cryptogramStorage: CryptogramStorage

    @Inject
    lateinit var cryptogramProvider: CryptogramProvider

    @Inject
    lateinit var activationCodeProvider: ActivationCodeProvider

    @Inject
    lateinit var onboardingStatusProvider: OnboardingStatusProvider

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<TestViewModel>

    @Inject
    lateinit var notificationHandler: NotificationHandler

    private val viewModel: TestViewModel by viewModels { viewModelFactory }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        sonar_id.text = sonarIdProvider.get()
        app_version.text = appVersion()

        val adapter = EventsAdapter()
        events.adapter = adapter
        events.layoutManager = LinearLayoutManager(this)

        setStates()

        setNotifications()

        continueButton.setOnClickListener {
            finish()
        }

        resetButton.setOnClickListener {
            userStateMachine.reset()
            sonarIdProvider.clear()
            onboardingStatusProvider.clear()
            activationCodeProvider.clear()
            cryptogramStorage.clear()
            viewModel.clear()
        }

        exportButton.setOnClickListener {
            viewModel.storeEvents(this)
        }

        viewModel.observeContactEvents().observe(this) {
            if (it.isEmpty()) no_events.visibility = View.VISIBLE
            else {
                no_events.visibility = View.GONE
                adapter.submitList(it)
            }
        }

        viewModel.observeCryptogram().observe(this) {
            val cryptogramBytes = it.asBytes()
            val (cryptogramColour, inverseColour) = cryptogramColourAndInverse(cryptogramBytes)
            encrypted_broadcast_id.text = Base64.encodeToString(cryptogramBytes, Base64.DEFAULT)
            encrypted_broadcast_id.setBackgroundColor(cryptogramColour)
            encrypted_broadcast_id.setTextColor(inverseColour)
        }
    }

    override fun onResume() {
        super.onResume()

        populateFirebaseId()

        updateCurrentState()
    }

    @SuppressLint("SetTextI18n")
    private fun updateCurrentState() {
        showCurrentState.text = when (val state = this.userStateMachine.state()) {
            is DefaultState -> "In default state"
            is ExposedState -> "Exposed: ${state.since.toUiFormat()} - ${state.until.toUiFormat()}"
            is SymptomaticState -> "Symptomatic:  ${state.since.toUiFormat()} - ${state.until.toUiFormat()}"
            is ExposedSymptomaticState -> "Exp-symptomatic:  ${state.since.toUiFormat()} - ${state.until.toUiFormat()}"
            is PositiveState -> "Positive: ${state.since.toUiFormat()} - ${state.until.toUiFormat()}"
        }
    }

    private fun populateFirebaseId() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                try {
                    val token = task.result?.token
                    firebase_token.text = token
                } catch (_: Exception) {
                }
            }
    }

    private fun showStateDatePicker(title: String, callback: (LocalDate) -> Unit) {
        DatePickerFragment(this, LocalDate.now()) {
            callback(it)
            updateCurrentState()
        }.show(supportFragmentManager, title)
    }

    private fun setStates() {
        setDefaultState.setOnClickListener {
            userStateMachine.reset()
            updateCurrentState()
        }

        setExposedState.setOnClickListener {
            showStateDatePicker("Exposure Date") {
                userStateMachine.reset()
                userStateMachine.transitionOnExposure(it.toUtcNormalized())
            }
        }

        setSymptomaticState.setOnClickListener {
            showStateDatePicker("Symptom Date") {
                userStateMachine.reset()
                userStateMachine.diagnose(it, nonEmptySetOf(COUGH, TEMPERATURE))
            }
        }

        setExposedSymptomaticState.setOnClickListener {
            showStateDatePicker("Exposure Date") {
                userStateMachine.reset()
                userStateMachine.transitionOnExposure(it.toUtcNormalized())
                userStateMachine.diagnose(it, nonEmptySetOf(TEMPERATURE))
            }
        }

        setPositiveState.setOnClickListener {
            showStateDatePicker("Test Date") {
                userStateMachine.reset()
                userStateMachine.transitionOnTestResult(
                    TestInfo(TestResult.POSITIVE, it.toUtcNormalized())
                )
            }
        }
    }

    private fun setNotifications() {
        setTestPositiveNotification.setOnClickListener {
            showStateDatePicker("Test Date") {
                notificationHandler.handleNewMessage(testResultMessageData(it, TestResult.POSITIVE))
            }
        }

        setTestNegativeNotification.setOnClickListener {
            showStateDatePicker("Test Date") {
                notificationHandler.handleNewMessage(testResultMessageData(it, TestResult.NEGATIVE))
            }
        }

        setTestInvalidNotification.setOnClickListener {
            showStateDatePicker("Test Date") {
                notificationHandler.handleNewMessage(testResultMessageData(it, TestResult.INVALID))
            }
        }

        setExposedNotification.setOnClickListener {
            showStateDatePicker("Exposure Date") {
                notificationHandler.handleNewMessage(exposedMessageData(it))
            }
        }
    }

    private fun testResultMessageData(date: LocalDate, result: TestResult): Map<String, String> {
        return mapOf(
            "type" to "Test Result",
            "result" to result.name,
            "testTimestamp" to date.toDateTime(LocalTime.now()).toUtcIsoFormat()
        )
    }

    private fun exposedMessageData(date: LocalDate): Map<String, String> {
        return mapOf(
            "type" to "Status Update",
            "status" to "Potential",
            "mostRecentProximityEventDate" to date.toDateTime(LocalTime.now()).toUtcIsoFormat()
        )
    }

    class DatePickerFragment(
        private val activity: Activity,
        private val since: LocalDate,
        private val callback: (LocalDate) -> Unit
    ) : DialogFragment(), DatePickerDialog.OnDateSetListener {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return DatePickerDialog(
                activity,
                this,
                since.year,
                since.monthOfYear - 1,
                since.dayOfMonth
            )
        }

        override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
            callback(LocalDate(year, month + 1, day))
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, TesterActivity::class.java)
    }
}
