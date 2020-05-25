/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View.FOCUS_DOWN
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import androidx.core.view.postDelayed
import androidx.core.widget.CompoundButtonCompat
import kotlinx.android.synthetic.main.activity_review_diagnosis.submit_diagnosis
import kotlinx.android.synthetic.main.activity_submit_diagnosis.confirmationCheckbox
import kotlinx.android.synthetic.main.activity_submit_diagnosis.needConfirmationHint
import kotlinx.android.synthetic.main.activity_submit_diagnosis.scrollView
import kotlinx.android.synthetic.main.symptom_banner.toolbar
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import javax.inject.Inject

class DiagnoseSubmitActivity : BaseActivity() {
    @Inject
    protected lateinit var userStateStorage: UserStateStorage

    @Inject
    lateinit var userInbox: UserInbox

    @Inject
    protected lateinit var reminders: Reminders

    private val symptoms: Set<Symptom> by lazy { intent.getSymptoms() }

    private val symptomsDate: LocalDate by lazy {
        DateTime(intent.getLongExtra(SYMPTOMS_DATE, 0), UTC).toLocalDate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_submit_diagnosis)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val checkboxColors = ContextCompat.getColorStateList(this, R.color.checkbox_colors)
        val checkboxErrorColors = ContextCompat.getColorStateList(this, R.color.colorDanger)
        CompoundButtonCompat.setButtonTintList(confirmationCheckbox, checkboxColors)

        confirmationCheckbox.setOnCheckedChangeListener { _, isChecked ->
            CompoundButtonCompat.setButtonTintList(confirmationCheckbox, checkboxColors)

            if (isChecked) {
                needConfirmationHint.visibility = INVISIBLE
            }
        }

        submit_diagnosis.setOnClickListener {
            if (!confirmationCheckbox.isChecked) {
                needConfirmationHint.visibility = VISIBLE
                scrollView.fullScroll(FOCUS_DOWN)
                needConfirmationHint.postDelayed(50) {
                    needConfirmationHint.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    CompoundButtonCompat.setButtonTintList(
                        confirmationCheckbox,
                        checkboxErrorColors
                    )
                }
                return@setOnClickListener
            }
            needConfirmationHint.visibility = INVISIBLE
            SubmitContactEventsWorker.schedule(this, symptomsDate, symptoms.toList())
            updateStateAndNavigate()
        }
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled) {
            submit_diagnosis.setBackgroundResource(R.drawable.button_round_background_inversed)
        } else {
            submit_diagnosis.setBackgroundResource(R.drawable.button_round_background)
        }
    }

    private fun updateStateAndNavigate() {
        val currentState = userStateStorage.get()
        val state = UserStateTransitions.diagnose(
            currentState,
            symptomsDate,
            NonEmptySet.create(symptoms)!!
        )

        if (state is DefaultState && symptoms.isNotEmpty()) {
            userInbox.addRecovery()
        }

        state.scheduleCheckInReminder(reminders)
        userStateStorage.set(state)

        Timber.d("Updated the state to: $state")

        navigateTo(state)
    }

    companion object {

        private const val SYMPTOMS_DATE = "SYMPTOMS_DATE"

        fun start(context: Context, symptoms: Set<Symptom>, symptomsDate: DateTime) =
            context.startActivity(getIntent(context, symptoms, symptomsDate))

        private fun getIntent(
            context: Context,
            symptoms: Set<Symptom>,
            symptomsDate: DateTime
        ) =
            Intent(context, DiagnoseSubmitActivity::class.java).apply {
                putSymptoms(symptoms)
                putExtra(SYMPTOMS_DATE, symptomsDate.millis)
            }
    }
}
