/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import com.google.firebase.iid.FirebaseInstanceId
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

typealias Token = String

interface TokenRetriever {
    suspend fun retrieveToken(): Token
}

class FirebaseTokenRetriever @Inject constructor() : TokenRetriever {
    override suspend fun retrieveToken(): Token {
        return suspendCoroutine { cont ->
            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener { task ->
                    try {
                        if (!task.isSuccessful) {
                            Timber.w(task.exception, "getInstanceId failed")
                            cont.resumeWithException(
                                task.exception ?: RuntimeException("Cannot retrieve Firebase Id")
                            )
                            return@addOnCompleteListener
                        }
                        val token = task.result?.token!!
                        cont.resume(token)
                    } catch (exception: Exception) {
                        // Can fail even if the status isSuccessful
                        // https://stackoverflow.com/questions/60698622/java-io-ioexception-fis-auth-error-in-android-firebase
                        Timber.w(exception, "getInstanceId failed")
                        cont.resumeWithException(exception)
                    }
                }
        }
    }
}
