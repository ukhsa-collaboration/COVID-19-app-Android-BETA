/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

fun <T, U> LiveData<T>.map(function: (T) -> U): LiveData<U> =
    Transformations.map(this, function)

fun <T> LiveData<T>.observe(owner: LifecycleOwner, function: (T) -> Unit) =
    observe(owner, Observer(function))
