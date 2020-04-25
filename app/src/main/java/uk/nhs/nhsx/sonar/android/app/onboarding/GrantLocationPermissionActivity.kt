package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseText
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.takeActionButton
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.locationPermissionsGranted

open class GrantLocationPermissionActivity :
    AppCompatActivity(R.layout.activity_edge_case) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 29) {
            edgeCaseTitle.setText(R.string.grant_location_permission_title)
            edgeCaseText.setText(R.string.grant_location_permission_rationale)
        } else {
            edgeCaseTitle.setText(R.string.grant_location_permission_title_pre_10)
            edgeCaseText.setText(R.string.grant_location_permission_rationale_pre_10)
        }
        takeActionButton.setText(R.string.go_to_app_settings)

        takeActionButton.setOnClickListener {
            val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (locationPermissionsGranted()) {
            finish()
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, GrantLocationPermissionActivity::class.java)
    }
}
