/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.edgecases

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_edge_case.banner
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseText
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.nhsPanel
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.onboarding.EnableBluetoothActivity

class ReEnableBluetoothActivity : EnableBluetoothActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        banner.isVisible = true
        nhsPanel.isVisible = false

        edgeCaseTitle.setText(R.string.re_enable_bluetooth_title)
        val appName = getString(R.string.app_name)
        val rationale = getString(R.string.re_enable_bluetooth_rationale, appName)
        edgeCaseText.text = rationale
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    override fun inflateBluetoothDescriptionLayout() {
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ReEnableBluetoothActivity::class.java)
    }
}
