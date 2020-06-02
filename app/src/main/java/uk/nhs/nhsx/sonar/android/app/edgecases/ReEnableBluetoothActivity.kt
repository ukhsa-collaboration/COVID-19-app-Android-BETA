/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.edgecases

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_edge_case.banner
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseContainer
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.nhsPanel
import kotlinx.android.synthetic.main.view_re_enable_bluetooth.reEnableBluetoothParagraph1
import kotlinx.android.synthetic.main.view_re_enable_bluetooth.reEnableBluetoothParagraph2
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.onboarding.EnableBluetoothActivity

class ReEnableBluetoothActivity : EnableBluetoothActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        banner.isVisible = true
        nhsPanel.isVisible = false

        edgeCaseTitle.setText(R.string.re_enable_bluetooth_title)
        inflateBluetoothDescriptionLayout()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    @SuppressLint("InflateParams")
    override fun inflateBluetoothDescriptionLayout() {
        val layout = layoutInflater.inflate(R.layout.view_re_enable_bluetooth, null)
        edgeCaseContainer.addView(layout)

        reEnableBluetoothParagraph1.text = rationaleDescription1()
        reEnableBluetoothParagraph2.text = rationaleDescription2()
    }

    private fun rationaleDescription1() =
        getString(R.string.re_enable_bluetooth_rationale_p1)

    private fun rationaleDescription2(): String {
        val appName = getString(R.string.app_name)
        return getString(R.string.re_enable_bluetooth_rationale_p2, appName)
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, ReEnableBluetoothActivity::class.java)
    }
}
