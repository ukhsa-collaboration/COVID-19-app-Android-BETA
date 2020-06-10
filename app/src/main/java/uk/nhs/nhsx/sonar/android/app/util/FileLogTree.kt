package uk.nhs.nhsx.sonar.android.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import timber.log.Timber
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class FileLogTree(private val context: Context) : Timber.DebugTree() {
    private val fileHandler: LogFileHandler? = try {
        LogFileHandler(context.filesDir.absolutePath)
    } catch (e: IllegalAccessException) {
        null
    }

    // When there is an exception at logging with Timber, it should not be used to log
    // this exception. Hence the Android Log function call in the catch block
    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val logTimeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(Date())
            try {
                fileHandler?.write("$logTimeStamp $tag $message \n")
        } catch (e: Exception) {
            Log.e(javaClass.name, "Error while logging into file : $e")
        }
    }
}
