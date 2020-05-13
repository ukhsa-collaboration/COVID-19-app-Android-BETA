/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.sonar.android.app.http.Promise
import javax.inject.Inject

class ReferenceCodeViewModel @Inject constructor(private val api: ReferenceCodeApi) : ViewModel() {

    sealed class State {
        object Loading : State()
        object Error : State()
        data class Loaded(val code: ReferenceCode) : State()
    }

    fun state(): LiveData<State> = api.generate().toLiveData()

    private fun Promise<ReferenceCode>.toLiveData(): LiveData<State> =
        MutableLiveData<State>(State.Loading).also { data ->

            this.onSuccess { data.value = State.Loaded(it) }
                .onError { data.value = State.Error }
        }
}
