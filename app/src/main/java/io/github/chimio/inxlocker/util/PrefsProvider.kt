package io.github.chimio.inxlocker.util

import com.highcapable.yukihookapi.hook.log.YLog
import de.robv.android.xposed.XSharedPreferences
import io.github.chimio.inxlocker.BuildConfig

object PrefsProvider {
    private const val PREFS_FILE_NAME = "selected_installer_package"

    private val sharedPrefs: XSharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        XSharedPreferences(BuildConfig.APPLICATION_ID, PREFS_FILE_NAME).apply {
            try {
                reload()
            } catch (e: Throwable) {
                YLog.e("PrefsProvider", "重载失败$e")
            }
        }
    }

    fun reload() {
        sharedPrefs.reload()
    }

    fun getString(key: String, defValue: String? = null): String? = try {
        sharedPrefs.getString(key, defValue)
    } catch (_: Throwable) {
        defValue
    }

    fun getBoolean(key: String, defValue: Boolean = false): Boolean = try {
        sharedPrefs.getBoolean(key, defValue)
    } catch (_: Throwable) {
        defValue
    }
}



