/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.isolate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.colocate.di.module.AppModule
import com.example.colocate.network.convert
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.Resident    IdProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.json.JSONArray
import timber.log.Timber.e
import timber.log.Timber.i
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationApi
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationData
import javax.inject.Inject
import javax.inject.Named

class IsolateViewModel @Inject constructor(
    private val colocationApi: CoLocationApi,
    private val contactEventDao: ContactEventDao,
    @Named(AppModule.DISPATCHER_IO) private val ioDispatcher: CoroutineDispatcher,
    private val residentIdProvider: ResidentIdProvider
) : ViewModel() {

    private val _isolationResult = MutableLiveData<Result>()
    val isolationResult: LiveData<Result> = _isolationResult

    fun onNotifyClick() {
        viewModelScope.launch(ioDispatcher) {
            val events: JSONArray =
                convert(contactEventDao.getAll())
            val coLocationData =
                CoLocationData(
                    residentIdProvider.getResidentId(), events
                )
            colocationApi.save(coLocationData,
                onSuccess = {
                    i("Success")
                    _isolationResult.value = Result.Success
                }, onError = {
                    e("Error: $it")
                    _isolationResult.value = Result.Error(it)
                })
        }
    }

    sealed class Result {
        object Success : Result()
        data class Error(val e: Exception) : Result()
    }
}
