/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import com.google.firebase.iid.FirebaseInstanceId
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

interface TokenRetriever {
    sealed class Result {
        data class Success(val token: String) : Result()
        data class Failure(val exception: Exception?) : Result()
    }

    suspend fun retrieveToken(): Result
}

class FirebaseTokenRetriever @Inject constructor() : TokenRetriever {
    override suspend fun retrieveToken(): TokenRetriever.Result {
        return suspendCoroutine { cont ->
            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Timber.w(task.exception, "getInstanceId failed")
                        cont.resumeWith(Result.success(TokenRetriever.Result.Failure(task.exception)))
                        return@addOnCompleteListener
                    }
                    val token = task.result?.token!!
                    cont.resumeWith(Result.success(TokenRetriever.Result.Success(token)))
                }
        }
    }
}
