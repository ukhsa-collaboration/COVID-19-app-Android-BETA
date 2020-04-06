/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.isolate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.colocate.BaseActivity
import com.example.colocate.R
import com.example.colocate.ViewModelFactory
import com.example.colocate.appComponent
import com.example.colocate.common.ViewState
import com.example.colocate.navigateTo
import com.example.colocate.status.StatusStorage
import kotlinx.android.synthetic.main.activity_isolate.isolate_notify
import javax.inject.Inject

class IsolateActivity : BaseActivity() {

    @Inject
    protected lateinit var viewModelFactory: ViewModelFactory<IsolateViewModel>

    @Inject
    protected lateinit var statusStorage: StatusStorage

    private val viewModel: IsolateViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_isolate)
        appComponent.inject(this)

        isolate_notify.setOnClickListener {
            viewModel.onNotifyClick()
        }

        viewModel.isolationResult.observe(this, Observer { result ->
            if (result is ViewState.Success) {
                Toast.makeText(
                    this,
                    getString(R.string.successfull_data_upload),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.erro_data_submission),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
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
            Intent(context, IsolateActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
