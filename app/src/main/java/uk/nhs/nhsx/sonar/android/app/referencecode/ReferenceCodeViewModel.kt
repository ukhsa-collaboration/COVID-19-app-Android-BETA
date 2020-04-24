package uk.nhs.nhsx.sonar.android.app.referencecode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.sonar.android.app.http.Promise
import javax.inject.Inject

class ReferenceCodeViewModel @Inject constructor(
    private val api: ReferenceCodeApi,
    private val provider: ReferenceCodeProvider
) : ViewModel() {

    sealed class State {
        object Loading : State()
        data class Loaded(val code: ReferenceCode) : State()
        object Error : State()
    }

    fun state(): LiveData<State> {
        val existingCode = provider.get()

        if (existingCode != null)
            return MutableLiveData(State.Loaded(existingCode))

        return api.generate().toLiveData()
    }

    private fun Promise<ReferenceCode>.toLiveData(): LiveData<State> {
        val liveData = MutableLiveData<State>(State.Loading)
        this
            .onSuccess {
                provider.set(it)
                liveData.value = State.Loaded(it)
            }
            .onError { liveData.value = State.Error }
        return liveData
    }
}
