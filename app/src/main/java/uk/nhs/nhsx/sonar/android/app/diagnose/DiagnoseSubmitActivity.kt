/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_review_diagnosis.submit_diagnosis
import kotlinx.android.synthetic.main.symptom_banner.close_btn
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.StateFactory
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import javax.inject.Inject

class DiagnoseSubmitActivity : BaseActivity() {
    @Inject
    protected lateinit var stateStorage: StateStorage

    @Inject
    protected lateinit var reminders: Reminders

    private val symptoms: Set<Symptom> by lazy {
        setOf(
            if (intent.getBooleanExtra(HAS_COUGH, false)) COUGH else null,
            if (intent.getBooleanExtra(HAS_TEMPERATURE, false)) TEMPERATURE else null
        ).filterNotNull().toSet()
    }

    private val symptomsDate: LocalDate by lazy {
        DateTime(intent.getLongExtra(SYMPTOMS_DATE, 0), UTC).toLocalDate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_submit_diagnosis)

        close_btn.setImageDrawable(getDrawable(R.drawable.ic_arrow_back))
        close_btn.setOnClickListener {
            onBackPressed()
        }

        submit_diagnosis.setOnClickListener {
            SubmitContactEventsWorker.schedule(this, symptomsDate)
            updateStateAndNavigate()
        }
    }

    private fun updateStateAndNavigate() {
        val state =
            NonEmptySet.create(symptoms)
                ?.let { StateFactory.decide(symptomsDate, it) }
                ?: DefaultState() // should never actually happen

        if (state is RedState) {
            reminders.scheduleCheckInReminder(state.until)
        }

        stateStorage.update(state)

        Timber.d("Updated the state to: $state")

        navigateTo(state)
    }

    companion object {

        private const val HAS_TEMPERATURE = "HAS_TEMPERATURE"
        private const val HAS_COUGH = "HAS_COUGH"
        private const val SYMPTOMS_DATE = "SYMPTOMS_DATE"

        fun start(
            context: Context,
            hasTemperature: Boolean = false,
            hasCough: Boolean = false,
            symptomsDate: DateTime
        ) =
            context.startActivity(
                getIntent(
                    context,
                    hasTemperature,
                    hasCough,
                    symptomsDate
                )
            )

        private fun getIntent(
            context: Context,
            hasTemperature: Boolean,
            hasCough: Boolean,
            symptomsDate: DateTime
        ) =
            Intent(context, DiagnoseSubmitActivity::class.java).apply {
                putExtra(HAS_COUGH, hasCough)
                putExtra(HAS_TEMPERATURE, hasTemperature)
                putExtra(SYMPTOMS_DATE, symptomsDate.millis)
            }
    }
}
