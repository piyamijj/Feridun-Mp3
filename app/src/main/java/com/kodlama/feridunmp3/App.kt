package com.kodlama.feridunmp3

import android.app.Application
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Çökme olduğunda yığın izini dosyaya yazar; bir sonraki açılışta ekranda gösterilir. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                File(filesDir, CRASH_FILE).writeText(
                    "SDK ${android.os.Build.VERSION.SDK_INT} / ${android.os.Build.MODEL}\n" +
                        "Thread: ${t.name}\n$sw"
                )
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(t, e)
        }
    }

    companion object {
        const val CRASH_FILE = "last_crash.txt"
    }
}
