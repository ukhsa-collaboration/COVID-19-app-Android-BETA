package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_enable_location_service.goToSettingsButton
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.isLocationEnabled

class EnableLocationServicesActivity :
    AppCompatActivity(R.layout.activity_enable_location_service) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        goToSettingsButton.setOnClickListener {
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
