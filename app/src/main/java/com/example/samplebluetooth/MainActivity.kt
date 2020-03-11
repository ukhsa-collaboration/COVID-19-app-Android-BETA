package com.example.samplebluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.pow


class MainActivity : AppCompatActivity() {

    private lateinit var mBluetoothLeScanner: BluetoothLeScanner
    private lateinit var mBluetoothLeAdvertiser: BluetoothLeAdvertiser
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private lateinit var locationStatus: TextView
    private lateinit var bluetoothStatus: TextView
    private lateinit var users: TextView
    private lateinit var beaconProximity: TextView
    private lateinit var beaconAccuracy: TextView

    private var mScanSettings: ScanSettings? = null
    private var mScanFilter: ScanFilter? = null
    private var mAdvertiseData: AdvertiseData? = null
    private var mAdvertiseSettings: AdvertiseSettings? = null


    private var mScanCallback: ScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            super.onScanResult(callbackType, result)

            Log.i("matches?", mScanFilter!!.matches(result).toString())
            Log.i("app", result.toString())
            val distance = calculateDistance(result.txPower, result.rssi)
            val distanceText = getDistance(distance)

            beaconProximity.text = "Bluetooth beacon proximity: $distanceText"
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.i("app", results.toString())
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.i("app", "scan failed with error: $errorCode")
        }
    }

    private var mAdvertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i("app", "advertising my beacon ${settingsInEffect.toString()}")
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Toast.makeText(
                    this,
                    "In order to use Bluetooth Low Energy, we need access to the device location.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ), 1
                )
            }
        }
        setContentView(R.layout.activity_main)
        locationStatus = findViewById(R.id.locationStatus)
        bluetoothStatus = findViewById(R.id.bluetoothStatus)
        users = findViewById(R.id.users)
        beaconProximity = findViewById(R.id.beaconProximity)
        beaconAccuracy = findViewById(R.id.beaconAccuracy)

        //TODO: Crashes if bluetooth is not turned on,
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothLeAdvertiser = mBluetoothAdapter.bluetoothLeAdvertiser
        mBluetoothLeScanner = mBluetoothAdapter.bluetoothLeScanner

        mScanFilter = scanFilter()
        mScanSettings = scanSettings()
        mAdvertiseData = advertiseData()
        mAdvertiseSettings = advertiseSettings()

        mBluetoothLeScanner.startScan(
            listOf(scanFilter()),
            mScanSettings,
            mScanCallback
        )
        mBluetoothLeAdvertiser.startAdvertising(
            mAdvertiseSettings,
            mAdvertiseData,
            mAdvertiseCallback
        );

    }

    fun calculateDistance(txPower: Int, rssi: Int): Double {
        if (rssi == 0) {
            return -1.0 // if we cannot determine accuracy, return -1.
        }
        val ratio = rssi * 1.0 / txPower
        return if (ratio < 1.0) {
            ratio.pow(10.0)
        } else {
            0.89976 * ratio.pow(7.7095) + 0.111
        }
    }

    private fun getDistance(accuracy: Double): String {
        return when {
            accuracy == -1.0 -> {
                "Unknown"
            }
            accuracy < 1 -> {
                "Immediate"
            }
            accuracy < 3 -> {
                "Near"
            }
            else -> {
                "Far"
            }
        }
    }


}
