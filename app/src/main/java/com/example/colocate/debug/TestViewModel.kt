package com.example.colocate.debug

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.colocate.ble.BleEvents
import com.example.colocate.persistence.ContactEventV2
import com.example.colocate.persistence.ContactEventV2Dao
import com.google.firebase.iid.FirebaseInstanceId
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TestViewModel(
    private val context: Context,
    private val contactEventDao: ContactEventV2Dao,
    private val eventTracker: BleEvents
) : ViewModel() {

    private val _eventsLiveData = MutableLiveData<List<ContactEventV2>>()
    val eventsLiveData: LiveData<List<ContactEventV2>> = _eventsLiveData

    fun clear() {
        viewModelScope.launch {
            contactEventDao.clearEvents()
            eventTracker.clear()
            withContext(Dispatchers.IO) {
                FirebaseInstanceId.getInstance().deleteInstanceId()
            }
            ProcessPhoenix.triggerRebirth(context)
        }
    }

    fun getEvents() {
        viewModelScope.launch {
            contactEventDao.getAll().apply {
                _eventsLiveData.value = this
            }
        }
    }

    fun observeConnectionEvents() = eventTracker.observeConnectionEvents()
}

class TestViewModelFactory(
    private val context: Context,
    private val contractEventDao: ContactEventV2Dao,
    private val eventTracker: BleEvents
) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TestViewModel(
            context,
            contractEventDao,
            eventTracker
        ) as T
    }
}
