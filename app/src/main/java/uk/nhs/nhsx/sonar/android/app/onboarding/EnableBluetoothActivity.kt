/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseContainer
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.takeActionButton
import kotlinx.android.synthetic.main.banner.toolbar_info
import kotlinx.android.synthetic.main.view_enable_bluetooth.enableBluetoothDescriptionContainer
import uk.nhs.nhsx.sonar.android.app.ColorInversionAwareActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothStateBroadcastReceiver
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.openUrl

open class EnableBluetoothActivity : ColorInversionAwareActivity(R.layout.activity_edge_case) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        edgeCaseTitle.setText(R.string.enable_bluetooth_title)
        inflateBluetoothDescriptionLayout()

        toolbar_info.setOnClickListener {
            openUrl(URL_INFO)
        }

        takeActionButton.setText(R.string.enable_bluetooth)

        takeActionButton.setOnClickListener {
            val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable()
            } else {
                finish()
            }
            takeActionButton.isEnabled = false
        }

        bluetoothStateBroadcastReceiver.register(this)
    }

    protected open fun inflateBluetoothDescriptionLayout() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val childLayout =
            inflater.inflate(R.layout.view_enable_bluetooth, enableBluetoothDescriptionContainer)
        edgeCaseContainer.addView(childLayout)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateBroadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        takeActionButton.isEnabled = true
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled) {
            takeActionButton.setBackgroundResource(R.drawable.button_round_background_inversed)
        } else {
            takeActionButton.setBackgroundResource(R.drawable.button_round_background)
        }
    }

    private val bluetoothStateBroadcastReceiver = BluetoothStateBroadcastReceiver { state ->
        if (state == BluetoothAdapter.STATE_ON) finish()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, EnableBluetoothActivity::class.java)
    }
}
