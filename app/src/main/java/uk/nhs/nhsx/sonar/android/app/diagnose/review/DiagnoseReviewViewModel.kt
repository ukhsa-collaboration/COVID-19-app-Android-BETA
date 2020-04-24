/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import timber.log.Timber.e
import timber.log.Timber.i
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.contactevents.CoLocationDataProvider
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import javax.inject.Inject
import javax.inject.Named

class DiagnoseReviewViewModel @Inject constructor(
    @Named(AppModule.DISPATCHER_IO) private val ioDispatcher: CoroutineDispatcher,
    private val coLocationApi: CoLocationApi,
    private val coLocationDataProvider: CoLocationDataProvider,
    private val sonarIdProvider: SonarIdProvider
) : ViewModel() {

    private val _isolationResult = MutableLiveData<ViewState>()
    val isolationResult: LiveData<ViewState> = _isolationResult

    fun uploadContactEvents(symptomsDate: DateTime) {
        viewModelScope.launch(ioDispatcher) {
            coLocationApi
                .save(
                    CoLocationData(
                        sonarId = sonarIdProvider.getSonarId(),
                        symptomsTimestamp = symptomsDate.toUtcIsoFormat(),
                        contactEvents = coLocationDataProvider.getEvents()
                    )
                )
                .onSuccess {
                    i("Success")
                    _isolationResult.value = ViewState.Success
                }
                .onError {
                    e("Error: $it")
                    _isolationResult.value = ViewState.Error
                }
        }
    }

    fun clearContactEvents() {
        viewModelScope.launch(ioDispatcher) {
            coLocationDataProvider.clearData()
        }
    }
}
