package com.evergreenpro.glasslayers.xposed.systemui

import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.hook.hookAllAfter
import java.util.concurrent.CopyOnWriteArrayList

object DozeTracker {

    private const val STATE_CONTROLLER = "com.android.systemui.statusbar.StatusBarStateControllerImpl"

    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val amountListeners = CopyOnWriteArrayList<() -> Unit>()

    @Volatile
    var dozeAmount = 0f
        private set

    @Volatile
    var isDozing = false
        private set

    fun install(): Int {
        var hooks = 0
        hooks += HookEnv.findClass(STATE_CONTROLLER).hookAllAfter("setDozeAmountInternal") { chain, _ ->
            val amount = chain.args.firstOrNull() as? Float ?: return@hookAllAfter
            if (amount != dozeAmount) {
                dozeAmount = amount
                amountListeners.forEach { it() }
            }
            val dozing = amount > 0f
            if (dozing == isDozing) return@hookAllAfter
            isDozing = dozing
            listeners.forEach { it() }
        }
        return hooks
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun addAmountListener(listener: () -> Unit) {
        amountListeners += listener
    }
}
