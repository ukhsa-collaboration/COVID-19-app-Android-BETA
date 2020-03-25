/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject

class ViewModelFactory<VM : ViewModel> @Inject constructor(private val viewModel: dagger.Lazy<VM>) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return viewModel.get() as T
    }
}
