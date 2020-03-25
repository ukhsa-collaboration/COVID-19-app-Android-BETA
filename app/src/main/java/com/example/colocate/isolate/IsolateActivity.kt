/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.isolate

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.colocate.ColocateApplication
import com.example.colocate.R
import com.example.colocate.ViewModelFactory
import kotlinx.android.synthetic.main.activity_isolate.*
import javax.inject.Inject

class IsolateActivity : AppCompatActivity() {

    @Inject
    protected lateinit var viewModelFactory: ViewModelFactory<IsolateViewModel>

    private val viewModel: IsolateViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_isolate)
        (application as ColocateApplication).applicationComponent.inject(this)

        isolate_notify.setOnClickListener {
            viewModel.onNotifyClick()
        }

        viewModel.isolationResult.observe(this, Observer {
        })
    }
}
