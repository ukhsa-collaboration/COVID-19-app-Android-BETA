/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber.e
import timber.log.Timber.i
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.persistence.CoLocationDataProvider
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationApi
import javax.inject.Inject
import javax.inject.Named

class DiagnoseReviewViewModel @Inject constructor(
    private val coLocationApi: CoLocationApi,
    @Named(AppModule.DISPATCHER_IO) private val ioDispatcher: CoroutineDispatcher,
    private val coLocationDataProvider: CoLocationDataProvider
) : ViewModel() {

    private val _isolationResult = MutableLiveData<ViewState>()
    val isolationResult: LiveData<ViewState> = _isolationResult

    fun uploadContactEvents() {
        viewModelScope.launch(ioDispatcher) {
            coLocationApi.save(
                coLocationDataProvider.getData(),
                onSuccess = {
                    i("Success")
                    _isolationResult.value = ViewState.Success
                }, onError = {
                    e("Error: $it")
                    _isolationResult.value = ViewState.Error(it)
                })
        }
    }

    fun clearContactEvents() {
        viewModelScope.launch(ioDispatcher) {
            coLocationDataProvider.clearData()
        }
    }
}
