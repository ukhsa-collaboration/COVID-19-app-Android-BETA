package com.example.colocate.registration

import android.app.Activity
import android.content.Context
import timber.log.Timber
import java.io.*
import java.lang.Exception


interface StateStorage {
    fun set()

    fun get()
}




fun witeObjectToFile(
    context: Context,
    obj: Any?,
    filename: String?
) {
    var objectOut: ObjectOutputStream? = null
    try {
        val fileOut: FileOutputStream = context.openFileOutput(filename, Activity.MODE_PRIVATE)
        objectOut = ObjectOutputStream(fileOut)
        objectOut.writeObject(obj)
        fileOut.fd.sync()
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (objectOut != null) {
            try {
                objectOut.close()
            } catch (e: IOException) {
                // do nowt
            }
        }
    }
}

fun readObjectFromFile(context: Context, filename: String?): Any? {
    return try {
        val fileIn: FileInputStream = context.applicationContext.openFileInput(filename)
        val objectIn = ObjectInputStream(fileIn)
        objectIn.use {
            it.readObject()
        }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}



