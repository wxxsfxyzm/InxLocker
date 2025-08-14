package io.github.chimio.inxlocker.util

import com.highcapable.yukihookapi.hook.log.YLog

private object LogSwitchProvider {
    private const val KEY_ENABLE = "enable_debug_log"

    fun isEnabled(): Boolean = try {
        PrefsProvider.getBoolean(KEY_ENABLE, true)
    } catch (_: Throwable) {
        true
    }
}

private const val DEFAULT_LOG_TAG = "InxLocker"

fun YLog.d(tag: String = DEFAULT_LOG_TAG, message: String) {
    if (LogSwitchProvider.isEnabled()) YLog.debug("[$tag] $message")
}

fun YLog.i(tag: String = DEFAULT_LOG_TAG, message: String) {
    if (LogSwitchProvider.isEnabled()) YLog.info("[$tag] $message")
}

fun YLog.w(tag: String = DEFAULT_LOG_TAG, message: String) {
    if (LogSwitchProvider.isEnabled()) YLog.warn("[$tag] $message")
}

fun YLog.e(tag: String = DEFAULT_LOG_TAG, message: String, throwable: Throwable? = null) {
    if (throwable != null) {
        YLog.error("[$tag] $message\n${throwable.stackTraceToString()}")
    } else {
        YLog.error("[$tag] $message")
    }
}


