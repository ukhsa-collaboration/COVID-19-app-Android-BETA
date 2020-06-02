/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.edgecases

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_edge_case.banner
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.nhsPanel
import kotlinx.android.synthetic.main.activity_edge_case.paragraphContainer
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.onboarding.EnableLocationActivity

class ReEnableLocationActivity : EnableLocationActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        banner.isVisible = true
        nhsPanel.isVisible = false

        edgeCaseTitle.setText(R.string.re_enable_location_title)
        paragraphContainer.addAllParagraphs(
            getString(R.string.re_enable_location_rationale_p1),
            getString(R.string.re_enable_location_rationale_p2),
            getString(R.string.re_enable_location_rationale_p3, getString(R.string.app_name))
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ReEnableLocationActivity::class.java)
    }
}
