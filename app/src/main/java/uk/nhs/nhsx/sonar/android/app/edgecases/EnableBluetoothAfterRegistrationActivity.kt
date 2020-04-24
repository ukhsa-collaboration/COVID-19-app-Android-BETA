package uk.nhs.nhsx.sonar.android.app.edgecases

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_enable_bluetooth.banner
import kotlinx.android.synthetic.main.activity_enable_bluetooth.nhsPanel
import uk.nhs.nhsx.sonar.android.app.onboarding.EnableBluetoothActivity

class EnableBluetoothAfterRegistrationActivity : EnableBluetoothActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        banner.isVisible = true
        nhsPanel.isVisible = false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, EnableBluetoothAfterRegistrationActivity::class.java)
    }
}