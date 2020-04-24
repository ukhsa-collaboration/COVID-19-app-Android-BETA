package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseText
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.takeActionButton
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.isLocationEnabled

class EnableLocationServicesActivity :
    AppCompatActivity(R.layout.activity_edge_case) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        edgeCaseTitle.setText(R.string.enable_location_service_title)
        edgeCaseText.setText(R.string.enable_location_service_rationale)
        takeActionButton.setText(R.string.go_to_your_settings)

        takeActionButton.setOnClickListener {
            val intent = Intent(ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isLocationEnabled()) {
            finish()
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, EnableLocationServicesActivity::class.java)
    }
}
