package io.github.chimio.inxlocker.util

import android.content.Intent
import com.highcapable.yukihookapi.hook.log.YLog

object IntentAnalyzer {
    sealed class Result {
        object ShouldRedirect : Result()
        object ShouldNotRedirect : Result()
    }

    fun analyze(intent: Intent): Result {
        return try {
            val TAG = "IntentAnalyzer"
            val original = IntentSnapshot.capture(intent)

            YLog.d(TAG,"Intent action: ${original.action}")
            YLog.d(TAG,"Intent component: ${original.component}")
            YLog.d(TAG,"package: ${original.packageName}")
            YLog.d(TAG,"Intent type: ${intent.type}")
            YLog.d(TAG,"Intent data: ${intent.data}")
            YLog.d(TAG,"Intent clipData: ${intent.clipData}")

            if (mimeTypeFromIntent(intent) || hasValidAction(intent) || mimeTypeFromCIntentData(intent)) {
                if (!hasSpecificComponent(intent)) {
                    if (intent.action == Intent.ACTION_DELETE) {
                        if (PrefsProvider.getBoolean("intercept_uninstall", false)) {
                            return Result.ShouldRedirect
                        } else {
                            return Result.ShouldNotRedirect
                        }
                    }
                    return Result.ShouldRedirect
                }
            }
            Result.ShouldNotRedirect
        } catch (_: Exception) {
            Result.ShouldNotRedirect
        }
    }

    private fun hasValidAction(intent: Intent): Boolean {
        return intent.action in listOf(
            "android.intent.action.INSTALL_PACKAGE",
            Intent.ACTION_DELETE)
    }

    private fun mimeTypeFromIntent(intent: Intent): Boolean {
        return intent.type == "application/vnd.android.package-archive"
    }
//字符串分析大法，不优雅，但是好像没什么问题
    private fun mimeTypeFromCIntentData(intent: Intent): Boolean {
        val uri = intent.data.toString()
        return (uri.endsWith(".apk") || uri.endsWith(".apks")|| uri.endsWith(".apk.1")) &&
                (uri.startsWith("file://") || uri.startsWith("content://"))
    }

    private fun hasSpecificComponent(intent: Intent): Boolean {
        return intent.component != null
    }
}
