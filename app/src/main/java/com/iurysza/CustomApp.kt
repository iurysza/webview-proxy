package com.iurysza

import android.app.Application
import android.util.Log

class CustomApp : Application() {
    override fun getPackageName(): String? {
        if (!WebviewPackageName.isRandom) return super.getPackageName()

        try {
            val stackTrace = Thread.currentThread().stackTrace
            for (element in stackTrace) {
                if ("org.chromium.base.BuildInfo".equals(element.className, ignoreCase = true)) {
                    if ("getAll".equals(element.methodName, ignoreCase = true)) {
                        log(stackTrace)
                        return "com.random.package"
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPackageName: ", e)
        }
        return super.getPackageName()
    }

    private fun log(stackTrace: Array<out StackTraceElement>) {
        Log.e(TAG, "getPackageName: ${stackTrace.reversed().map { "${it.className}:${it.methodName}\n" }}")
    }

}

const val TAG = "CustomApp"

object WebviewPackageName {
    var isRandom = false
}
