/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivationCodeObserver @Inject constructor() {
    private var savedActivationCode: String? = null
    private var listener: ((String) -> Unit)? = null

    fun setListener(listener: ((String) -> Unit)?) {
        this.listener = listener
        tryNotifyListener()
    }

    fun onGetActivationCode(activationCode: String) {
        this.savedActivationCode = activationCode
        tryNotifyListener()
    }

    private fun tryNotifyListener() {
        val activationCode = savedActivationCode
        if (activationCode != null && listener != null) {
            listener?.invoke(activationCode)
            savedActivationCode = null
        }
    }

    fun removeListener() {
        this.listener = null
    }
}
