package com.example.colocate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.colocate.persistence.ContactEventV2
import com.example.colocate.persistence.ContactEventV2Dao
import kotlinx.coroutines.launch

class TestViewModel(private val contractEventDao: ContactEventV2Dao) : ViewModel() {

    private val eventsLiveData = MutableLiveData<List<ContactEventV2>>()

    fun observeEvents(): LiveData<List<ContactEventV2>> = eventsLiveData

    fun clear() {
        viewModelScope.launch {
            contractEventDao.clearEvents()
        }
    }

    fun getEvents() {
        viewModelScope.launch {
            contractEventDao.getAll().apply {
                eventsLiveData.value = this
            }
        }
    }
}

class TestViewModelFactory(private val contractEventDao: ContactEventV2Dao) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TestViewModel(contractEventDao) as T
    }
}
