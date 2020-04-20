package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import uk.nhs.nhsx.sonar.android.app.R

class SimpleDiagnoseTemperatureActivity : DiagnoseTemperatureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setProgress(getString(R.string.progress_half))
    }

    override fun nextStep(hasTemperature: Boolean) {
        SimpleDiagnoseCoughActivity.start(this, hasTemperature)
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, SimpleDiagnoseTemperatureActivity::class.java)
    }
}
