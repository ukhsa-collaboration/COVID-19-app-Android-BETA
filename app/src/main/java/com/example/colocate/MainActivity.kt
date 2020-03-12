package com.example.colocate

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val confirm = findViewById<AppCompatButton>(R.id.confirm_onboarding)
        confirm.setOnClickListener {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1
            && grantResults.size == 2
            && grantResults.first() == PERMISSION_GRANTED
            && grantResults.last() == PERMISSION_GRANTED
        ) {
            startActivity(Intent(this, DiagnoseActivity::class.java))
        }
    }
}
