package com.evergreenpro.glasslayers.xposed.apps

import android.content.pm.ApplicationInfo
import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.prefs.ModulePrefs
import com.evergreenpro.glasslayers.xposed.systemui.ButtonGlass
import com.evergreenpro.glasslayers.xposed.systemui.GlassConfig
import com.evergreenpro.glasslayers.xposed.systemui.InstallerDialogGlass
import com.evergreenpro.glasslayers.xposed.systemui.KeyboardGlass
import com.evergreenpro.glasslayers.xposed.systemui.SettingsCardGlass
import com.evergreenpro.glasslayers.xposed.systemui.SliderGlass
import com.evergreenpro.glasslayers.xposed.systemui.SwitchGlass
import com.evergreenpro.glasslayers.xposed.systemui.ViewGlassUnderlay
import java.util.concurrent.atomic.AtomicBoolean

object AppModule {

    val PACKAGES = setOf(
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.inputmethod.latin"
    )

    private val started = AtomicBoolean(false)

    private val installers: List<Pair<String, () -> Int>> = listOf(
        "ViewGlassUnderlay" to ViewGlassUnderlay::install,
        "SwitchGlass" to SwitchGlass::install,
        "SliderGlass" to SliderGlass::install,
        "ButtonGlass" to ButtonGlass::install,
        "SettingsCardGlass" to SettingsCardGlass::install,
        "InstallerDialogGlass" to InstallerDialogGlass::install,
        "KeyboardGlass" to KeyboardGlass::install
    )

    fun claim(): Boolean = started.compareAndSet(false, true)

    fun start(info: ApplicationInfo?) {
        GlassConfig.liveGlass = false
        ModulePrefs.init(HookEnv.module, info?.deviceProtectedDataDir)
        GlassConfig.reload()
        ModulePrefs.addListener {
            GlassConfig.reload()
            try {
                SwitchGlass.refresh()
                SliderGlass.refresh()
                ButtonGlass.refresh()
                SettingsCardGlass.refresh()
                InstallerDialogGlass.refresh()
                KeyboardGlass.refresh()
                ViewGlassUnderlay.refresh()
            } catch (throwable: Throwable) {
                HookEnv.log("Refresh failed", throwable)
            }
        }
        installers.forEach { (name, install) ->
            try {
                HookEnv.owner = name
                val hooks = install()
                HookEnv.log(if (hooks > 0) "App $name: $hooks hooks" else "App $name: no hooks matched")
            } catch (throwable: Throwable) {
                HookEnv.log("App $name install failed", throwable)
            }
        }
        HookEnv.owner = ""
    }
}
