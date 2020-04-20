package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_simple_cough_diagnosis.confirm_diagnosis
import kotlinx.android.synthetic.main.cough_questionnaire_layout.cough_diagnosis_answer
import kotlinx.android.synthetic.main.cough_questionnaire_layout.radio_selection_error
import kotlinx.android.synthetic.main.symptom_banner.close_btn
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.navigateTo
import javax.inject.Inject

class SimpleDiagnoseCoughActivity : AppCompatActivity() {

    private val hasTemperature: Boolean by lazy {
        intent.getBooleanExtra(DiagnoseCoughActivity.HAS_TEMPERATURE, false)
    }

    @Inject
    protected lateinit var stateStorage: StateStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        setContentView(R.layout.activity_simple_cough_diagnosis)
        close_btn.setImageDrawable(getDrawable(R.drawable.ic_arrow_back))
        close_btn.setOnClickListener {
            onBackPressed()
        }

        confirm_diagnosis.setOnClickListener {
            when (cough_diagnosis_answer.checkedRadioButtonId) {
                R.id.yes -> {
                    nextStep(hasTemperature, true)
                }
                R.id.no -> {
                    nextStep(hasTemperature, false)
                }
                else -> {
                    radio_selection_error.visibility = View.VISIBLE
                    radio_selection_error.announceForAccessibility(getString(R.string.radio_button_cough_error))
                }
            }
        }

        cough_diagnosis_answer.setOnCheckedChangeListener { _, _ ->
            radio_selection_error.visibility = View.GONE
        }
    }

    private fun nextStep(hasTemperature: Boolean, hasCough: Boolean) =
        when {
            hasTemperature and hasCough -> RedState(
                DateTime.now(UTC).plusDays(1),
                setOf(Symptom.TEMPERATURE, Symptom.COUGH)
            )
            hasTemperature -> RedState(
                DateTime.now(UTC).plusDays(1),
                setOf(Symptom.TEMPERATURE)
            )
            hasCough -> DefaultState(DateTime.now(UTC).plusDays(1)) // TODO show dialog
            else -> DefaultState(DateTime.now(UTC).plusDays(1))
        }.apply {
            stateStorage.update(this)
            navigateTo(this)
        }

    companion object {

        private const val HAS_TEMPERATURE = "HAS_TEMPERATURE"

        fun start(context: Context, hasTemperature: Boolean = false) =
            context.startActivity(getIntent(context, hasTemperature))

        private fun getIntent(context: Context, hasTemperature: Boolean) =
            Intent(context, SimpleDiagnoseCoughActivity::class.java).apply {
                putExtra(HAS_TEMPERATURE, hasTemperature)
            }
    }
}
