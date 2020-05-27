package uk.nhs.nhsx.sonar.android.app.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings


fun Activity.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri

    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
}
