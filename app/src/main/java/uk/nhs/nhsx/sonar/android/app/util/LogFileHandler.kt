package uk.nhs.nhsx.sonar.android.app.util

import android.os.Environment
import java.io.File
import java.io.FileWriter

class LogFileHandler(dataDirectory: String) {
    private val path = "log"
    private val fileName = "app.log"
    private var file: File

    init {
        this.file = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val root = File(
                dataDirectory,
                path)

            if (!root.exists()) {
                root.mkdirs()
            }
            File(root, fileName)
        } else {
            throw IllegalAccessException("No writeable filesystem available.")
        }
    }

    fun write(line: String) {
        FileWriter(file, true).apply {
            append(line)
            flush()
            close()
        }
    }

    fun readAllBytes(): ByteArray? = if (file.exists()) file.readBytes() else null

    fun delete() = file.delete()
}
