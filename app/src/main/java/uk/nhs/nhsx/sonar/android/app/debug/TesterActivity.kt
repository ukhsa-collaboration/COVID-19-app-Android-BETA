/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.debug

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.android.synthetic.main.activity_test.sonar_id
import org.joda.time.DateTime
import org.joda.time.LocalDate
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.appVersion
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramProvider
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramStorage
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationHandler
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedSymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import uk.nhs.nhsx.sonar.android.app.util.observe
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import javax.inject.Inject

fun cryptogramColourAndInverse(cryptogramBytes: ByteArray): Pair<Int, Int> {
    val r = cryptogramBytes[0].toInt()
    val g = cryptogramBytes[1].toInt()
    val b = cryptogramBytes[2].toInt()
    return Pair(Color.rgb(r, g, b), Color.rgb(255 - r, 255 - g, 255 - b))
}

class TesterActivity : AppCompatActivity(R.layout.activity_test) {

    @Inject
    lateinit var userStateStorage: UserStateStorage

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

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        sonar_id.text = sonarIdProvider.get()
        app_version.text = appVersion()
        if (cryptogramProvider.canProvideCryptogram()) {
            val cryptogramBytes = cryptogramProvider.provideCryptogram().asBytes()
            val (cryptogramColour, inverseColour) = cryptogramColourAndInverse(cryptogramBytes)
            encrypted_broadcast_id.text = Base64.encodeToString(cryptogramBytes, Base64.DEFAULT)
            encrypted_broadcast_id.setBackgroundColor(cryptogramColour)
            encrypted_broadcast_id.setTextColor(inverseColour)
        } else {
            encrypted_broadcast_id.text = "Cannot generate cryptogram"
        }

        val adapter = EventsAdapter()
        events.adapter = adapter
        events.layoutManager = LinearLayoutManager(this)

        setStates()

        setNotifications()

        continueButton.setOnClickListener {
            finish()
        }

        resetButton.setOnClickListener {
            userStateStorage.clear()
            sonarIdProvider.clear()
            onboardingStatusProvider.clear()
            activationCodeProvider.clear()
            cryptogramStorage.clear()
            viewModel.clear()
        }

        exportButton.setOnClickListener {
            viewModel.storeEvents(this)
        }

        viewModel.observeConnectionEvents().observe(this) {
            Timber.d("Devices are $it")
            if (it.isEmpty()) no_events.visibility = View.VISIBLE
            else {
                no_events.visibility = View.GONE
                val ids = it.map { event -> event.cryptogram }.distinct()
                val unique = ids.map { id -> it.findLast { event -> event.cryptogram == id } }
                adapter.submitList(unique)
            }
        }

        viewModel.observeConnectionEvents()

        populateFirebaseId()
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

    private fun setStates() {
        setDefaultState.setOnClickListener {
            userStateStorage.set(DefaultState)
            finish()
        }

        setExposedState.setOnClickListener {
            userStateStorage.set(UserState.exposed(LocalDate.now()))
            finish()
        }

        setSymptomaticState.setOnClickListener {
            userStateStorage.set(
                UserState.symptomatic(
                    symptomsDate = LocalDate.now(),
                    symptoms = nonEmptySetOf(COUGH, TEMPERATURE)
                )
            )
            finish()
        }

        setExposedSymptomaticState.setOnClickListener {
            val exposed = UserState.exposed(LocalDate.now())
            userStateStorage.set(
                ExposedSymptomaticState(
                    since = exposed.since,
                    until = exposed.until,
                    symptoms = nonEmptySetOf(COUGH, TEMPERATURE)
                )
            )
            finish()
        }

        setPositiveState.setOnClickListener {
            userStateStorage.set(
                UserState.positive(
                    testDate = DateTime.now(),
                    symptoms = nonEmptySetOf(COUGH, TEMPERATURE)
                )
            )
            finish()
        }
    }

    private fun setNotifications() {
        setTestPositiveNotification.setOnClickListener {
            notificationHandler.handleNewMessage(testResultMessageData(TestResult.POSITIVE))
        }

        setTestNegativeNotification.setOnClickListener {
            notificationHandler.handleNewMessage(testResultMessageData(TestResult.NEGATIVE))
        }

        setTestInvalidNotification.setOnClickListener {
            notificationHandler.handleNewMessage(testResultMessageData(TestResult.INVALID))
        }

        setExposedNotification.setOnClickListener {
            notificationHandler.handleNewMessage(exposedMessageData())
        }
    }

    private fun testResultMessageData(result: TestResult): Map<String, String> {
        return mapOf(
            "type" to "Test Result",
            "result" to result.name,
            "testTimestamp" to DateTime.now().toUtcIsoFormat()
        )
    }

    private fun exposedMessageData(): Map<String, String> {
        return mapOf(
            "type" to "Status Update",
            "status" to "Potential"
        )
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, TesterActivity::class.java)
    }
}
