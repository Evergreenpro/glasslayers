package com.evergreenpro.glasslayers.xposed.systemui

import com.evergreenpro.glasslayers.xposed.hook.call
import com.evergreenpro.glasslayers.xposed.hook.field

internal object ExpandableTransitions {

    private const val DIALOG_SHOWING = "isDialogShowing\$delegate"
    private const val ANIMATING = "isAnimating\$delegate"

    fun isTransitioning(controller: Any?): Boolean {
        val target = controller ?: return false
        return target.field(DIALOG_SHOWING)?.call("getValue") == true || target.field(ANIMATING)?.call("getValue") == true
    }
}
