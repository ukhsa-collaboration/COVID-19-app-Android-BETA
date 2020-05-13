/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import javax.inject.Inject
import javax.inject.Named

class ReferenceCodeViewModel @Inject constructor(
    @Named(AppModule.DISPATCHER_IO)
    private val ioDispatcher: CoroutineDispatcher,
    private val referenceCodeApi: ReferenceCodeApi,
    private val sonarIdProvider: SonarIdProvider
) : ViewModel() {

    private val liveData = MutableLiveData<State>(State.Loading)

    fun getReferenceCode() {
        viewModelScope.launch {

            liveData.postValue(State.Loading)

            if (sonarIdProvider.hasProperSonarId()) {
                withContext(ioDispatcher) {
                    referenceCodeApi.get(sonarIdProvider.get())
                        .onSuccess { liveData.postValue(State.Loaded(it)) }
                        .onError { liveData.postValue(State.Error) }
                }
            } else {
                liveData.postValue(State.Error)
            }
        }
    }

    fun state(): LiveData<State> = liveData

    sealed class State {
        object Loading : State()
        object Error : State()
        data class Loaded(val code: ReferenceCode) : State()
    }
}
