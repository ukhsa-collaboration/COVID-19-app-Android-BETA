package uk.nhs.nhsx.sonar.android.app.edgecases

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_edge_case.banner
import kotlinx.android.synthetic.main.activity_edge_case.nhsPanel
import uk.nhs.nhsx.sonar.android.app.onboarding.EnableLocationActivity

class EnableLocationAfterRegistrationActivity : EnableLocationActivity() {

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
            Intent(context, EnableLocationAfterRegistrationActivity::class.java)
    }
}