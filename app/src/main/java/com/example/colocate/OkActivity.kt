/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.colocate.status.StatusStorage
import javax.inject.Inject

class OkActivity : AppCompatActivity() {

    @Inject
    protected lateinit var statusStorage: StatusStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_ok)

        findViewById<AppCompatButton>(R.id.re_diagnose_button).setOnClickListener {
            DiagnoseActivity.start(this)
        }
    }

    override fun onResume() {
        super.onResume()

        navigateTo(statusStorage.get())
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(getIntent(context))
        }

        private fun getIntent(context: Context) =
            Intent(context, OkActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
