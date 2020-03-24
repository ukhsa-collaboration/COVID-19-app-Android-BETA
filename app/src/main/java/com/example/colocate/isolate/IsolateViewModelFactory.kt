package com.example.colocate.isolate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.colocate.di.AppModule
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ResidentIdProvider
import kotlinx.coroutines.CoroutineDispatcher
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import javax.inject.Inject
import javax.inject.Named

class IsolateViewModelFactory @Inject constructor(
    private val httpClient: HttpClient,
    private val contactEventDao: ContactEventDao,
    @Named(AppModule.DISPATCHER_IO) private val ioDispatcher: CoroutineDispatcher,
    private val residentIdProvider: ResidentIdProvider
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return IsolateViewModel(httpClient, contactEventDao, ioDispatcher, residentIdProvider) as T
    }
}