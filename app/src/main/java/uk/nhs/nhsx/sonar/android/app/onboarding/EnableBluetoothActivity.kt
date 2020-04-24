package uk.nhs.nhsx.sonar.android.app.onboarding

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_enable_bluetooth.enableBluetoothButton
import uk.nhs.nhsx.sonar.android.app.R

open class EnableBluetoothActivity : AppCompatActivity(R.layout.activity_enable_bluetooth) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableBluetoothButton.setOnClickListener {
            val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable()
            } else {
                finish()
            }
            enableBluetoothButton.isEnabled = false
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateBroadcastReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateBroadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        enableBluetoothButton.isEnabled = true
    }

    private val bluetoothStateBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                if (state == BluetoothAdapter.STATE_ON) {
                    bluetoothHasBeenEnabled()
                }
            }
        }
    }

    private fun bluetoothHasBeenEnabled() {
        finish()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, EnableBluetoothActivity::class.java)
    }
}
