package com.drdisagree.glasslayer.xposed.apps

import com.drdisagree.glasslayer.xposed.hook.HookEnv
import com.drdisagree.glasslayer.xposed.prefs.ModulePrefs
import com.drdisagree.glasslayer.xposed.systemui.ButtonGlass
import com.drdisagree.glasslayer.xposed.systemui.GlassConfig
import com.drdisagree.glasslayer.xposed.systemui.InstallerDialogGlass
import com.drdisagree.glasslayer.xposed.systemui.SettingsCardGlass
import com.drdisagree.glasslayer.xposed.systemui.SliderGlass
import com.drdisagree.glasslayer.xposed.systemui.SwitchGlass
import com.drdisagree.glasslayer.xposed.systemui.ViewGlassUnderlay
import java.util.concurrent.atomic.AtomicBoolean

object AppModule {

    val PACKAGES = setOf(
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    private val started = AtomicBoolean(false)

    private val installers: List<Pair<String, () -> Int>> = listOf(
        "ViewGlassUnderlay" to ViewGlassUnderlay::install,
        "SwitchGlass" to SwitchGlass::install,
        "SliderGlass" to SliderGlass::install,
        "ButtonGlass" to ButtonGlass::install,
        "SettingsCardGlass" to SettingsCardGlass::install,
        "InstallerDialogGlass" to InstallerDialogGlass::install
    )

    fun claim(): Boolean = started.compareAndSet(false, true)

    fun start() {
        GlassConfig.liveGlass = false
        ModulePrefs.init(HookEnv.module)
        GlassConfig.reload()
        ModulePrefs.addListener {
            GlassConfig.reload()
            try {
                SwitchGlass.refresh()
                SliderGlass.refresh()
                ButtonGlass.refresh()
                SettingsCardGlass.refresh()
                InstallerDialogGlass.refresh()
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
