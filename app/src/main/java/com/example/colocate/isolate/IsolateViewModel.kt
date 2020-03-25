package com.example.colocate.isolate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.colocate.di.module.AppModule
import com.example.colocate.network.convert
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ResidentIdProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber.e
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
        viewModelScope.launch {
            val events = withContext(ioDispatcher) {
                convert(contactEventDao.getAll())
            }
            val coLocationData =
                CoLocationData(
                    residentIdProvider.getResidentId(), events
                )
            colocationApi.save(coLocationData,
                onSuccess = {
                    _isolationResult.value = Result.Success
                }, onError = {
                    e(it)
                    _isolationResult.value = Result.Error(it)
                })
        }
    }

    sealed class Result {
        object Success : Result()
        data class Error(val e: Exception) : Result()
    }
}
