package io.github.chimio.inxlocker.util

import android.content.ComponentName
import android.content.Intent

data class IntentSnapshot(
    val action: String?,
    val component: ComponentName?,
    val packageName: String?
) {
    companion object {
        fun capture(intent: Intent) = IntentSnapshot(
            action = intent.action,
            component = intent.component,
            packageName = intent.`package`
        )
    }
}