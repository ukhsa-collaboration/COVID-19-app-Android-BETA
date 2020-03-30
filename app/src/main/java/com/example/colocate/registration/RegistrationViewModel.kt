package com.example.colocate.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.colocate.common.ViewState
import kotlinx.coroutines.launch
import javax.inject.Inject

class RegistrationViewModel @Inject constructor(
    private val registrationUseCase: RegistrationUseCase
) :
    ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> {
        return viewState
    }

    fun register() {
        viewModelScope.launch {
            viewState.value = ViewState.Progress
            val registrationResult = registrationUseCase.register()
            viewState.value = when (registrationResult) {
                RegistrationResult.Success, RegistrationResult.AlreadyRegistered -> ViewState.Success
                is RegistrationResult.Failure -> ViewState.Error(registrationResult.exception)
            }
        }
    }
}
