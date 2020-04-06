package com.example.colocate.debug

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.colocate.ble.BleEvents
import com.example.colocate.persistence.ContactEventV2
import com.example.colocate.persistence.ContactEventV2Dao
import kotlinx.coroutines.launch

class TestViewModel(
    private val contactEventDao: ContactEventV2Dao,
    private val eventTracker: BleEvents
) : ViewModel() {

    private val _eventsLiveData = MutableLiveData<List<ContactEventV2>>()
    val eventsLiveData: LiveData<List<ContactEventV2>> = _eventsLiveData

    fun clear() {
        viewModelScope.launch {
            contactEventDao.clearEvents()
            eventTracker.clear()
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
    private val contractEventDao: ContactEventV2Dao,
    private val eventTracker: BleEvents
) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TestViewModel(
            contractEventDao,
            eventTracker
        ) as T
    }
}
