/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_explanation.explanation_back

class ExplanationActivity : AppCompatActivity(R.layout.activity_explanation) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        explanation_back.setOnClickListener {
            onBackPressed()
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(getIntent(context))
        }

        private fun getIntent(context: Context) =
            Intent(context, ExplanationActivity::class.java)
    }
}
