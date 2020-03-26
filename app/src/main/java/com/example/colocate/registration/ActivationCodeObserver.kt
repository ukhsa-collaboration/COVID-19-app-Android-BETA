package com.example.colocate.registration

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivationCodeObserver @Inject constructor() {
    private var listener: ((String) -> Unit)? = null

    fun setListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    fun onGetActivationCode(activationCode: String) {
        listener?.invoke(activationCode)
    }
}
