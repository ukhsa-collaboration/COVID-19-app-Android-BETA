/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import kotlinx.android.synthetic.main.white_banner.toolbar
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.startStatusActivity
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.scrollToView
import uk.nhs.nhsx.sonar.android.app.util.setNavigateUpToolbar
import javax.inject.Inject

class DiagnoseSubmitActivity : BaseActivity() {
    @Inject
    protected lateinit var userStateStorage: UserStateStorage

    private val symptoms: Set<Symptom> by lazy { intent.getSymptoms() }

    private val symptomsDate: LocalDate by lazy {
        DateTime(intent.getLongExtra(SYMPTOMS_DATE, 0), UTC).toLocalDate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_submit_diagnosis)

        setNavigateUpToolbar(toolbar, R.string.add_my_symptoms)

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
                needConfirmationHint.announceForAccessibility(getString(R.string.please_confirm_the_information))
                scrollView.scrollToView(needConfirmationHint)
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
        userStateStorage.diagnose(symptomsDate, NonEmptySet.create(symptoms)!!)
        startStatusActivity()
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
