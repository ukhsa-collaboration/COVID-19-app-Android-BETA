/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.colocate.status.StatusStorage
import javax.inject.Inject

class AtRiskActivity : BaseActivity() {

    @Inject
    protected lateinit var statusStorage: StatusStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setContentView(R.layout.activity_at_risk)
    }

    override fun onResume() {
        super.onResume()

        navigateTo(statusStorage.get())
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(getIntent(context))
        }

        fun getIntent(context: Context) =
            Intent(context, AtRiskActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
