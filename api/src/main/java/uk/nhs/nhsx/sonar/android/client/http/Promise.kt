/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client.http

import uk.nhs.nhsx.sonar.android.client.http.Promise.State.Failed
import uk.nhs.nhsx.sonar.android.client.http.Promise.State.InProgress
import uk.nhs.nhsx.sonar.android.client.http.Promise.State.Succeeded

class Promise<T : Any?> private constructor() {

    sealed class State<T> {
        class InProgress<T> : State<T>()
        data class Succeeded<T>(val value: T) : State<T>()
        data class Failed<T>(val error: Exception) : State<T>()
    }

    private var state: State<T> = InProgress()

    val isInProgress get() = state is InProgress
    val isSuccess get() = state is Succeeded
    val isFailed get() = state is Failed

    val value
        get() =
            when (val s = state) {
                is Succeeded -> s.value
                else -> null
            }

    val error
        get() =
            when (val s = state) {
                is Failed -> s.error
                else -> null
            }

    private var successCallback: ((T) -> Unit)? = null
    private var errorCallback: ((Exception) -> Unit)? = null

    fun onSuccess(callback: (T) -> Unit): Promise<T> {
        successCallback = callback
        trigger()
        return this
    }

    fun onError(callback: (Exception) -> Unit): Promise<T> {
        errorCallback = callback
        trigger()
        return this
    }

    fun <U : Any?> map(mapping: (T) -> U): Promise<U> {
        val deferred = Deferred<U>()
        onSuccess { deferred.resolve(mapping(it)) }
        onError { deferred.fail(it) }
        return deferred.promise
    }

    fun mapToUnit(): Promise<Unit> =
        map {}

    private var triggered = false

    private fun trigger() {
        if (triggered) return

        when (val s = state) {
            is Succeeded -> {
                successCallback?.let {
                    triggered = true
                    it(s.value)
                }
            }
            is Failed -> {
                errorCallback?.let {
                    triggered = true
                    it(s.error)
                }
            }
        }
    }

    class Deferred<T : Any?> {

        val promise = Promise<T>()

        fun resolve(value: T) {
            promise.state = Succeeded(value)
            promise.trigger()
        }

        fun fail(error: Exception) {
            promise.state = Failed(error)
            promise.trigger()
        }
    }
}
