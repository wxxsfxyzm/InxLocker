package io.github.chimio.inxlocker.util

import android.content.Intent
import com.highcapable.yukihookapi.hook.log.YLog

object IntentRedirector {

    fun reloadPrefs() = PrefsProvider.reload()

    private fun getSelectedInstallerPackage(): String? {
        val value = PrefsProvider.getString("selected_installer_package", "")
        return if (value.isNullOrBlank()) null else value
    }

    private const val ACTION_INSTALL_PACKAGE = "android.intent.action.INSTALL_PACKAGE"
    private const val TAG = "InstallerRedirect"

    fun redirect(intent: Intent, tag: String = TAG) {
        try {
            IntentSnapshot.capture(intent)
            applyRedirection(intent)
            logRedirection(intent, tag)
        } catch (e: Exception) {
            YLog.e(tag, "重定向Intent 错误: ${e.message}", e)
        }
    }

    private fun applyRedirection(intent: Intent) {
        val targetPackage = getSelectedInstallerPackage()
        if (!targetPackage.isNullOrBlank()) {
            intent.component = null
            intent.setPackage(targetPackage)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        normalizeAction(intent)
    }

    private fun normalizeAction(intent: Intent) {
        when {
            intent.action == ACTION_INSTALL_PACKAGE -> intent.action = Intent.ACTION_VIEW
            intent.action.isNullOrEmpty() -> intent.action = Intent.ACTION_VIEW
        }
    }

    private fun logRedirection(current: Intent, tag: String) {
        YLog.i(tag, "Intent重定向:")
        YLog.i(tag, "- 目标 package: ${current.`package` ?: "<系统默认>"}")
    }
}