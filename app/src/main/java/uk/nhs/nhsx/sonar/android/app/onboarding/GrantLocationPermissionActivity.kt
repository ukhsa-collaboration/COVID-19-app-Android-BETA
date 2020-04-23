package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_grant_location_permission.goToSettingsButton
import uk.nhs.nhsx.sonar.android.app.R

class GrantLocationPermissionActivity :
    AppCompatActivity(R.layout.activity_grant_location_permission) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        goToSettingsButton.setOnClickListener {
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

    private fun locationPermissionsGranted(): Boolean {
        return PermissionActivity.locationPermissions.all { permission ->
            packageManager.checkPermission(
                permission,
                packageName
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, GrantLocationPermissionActivity::class.java)
    }
}
