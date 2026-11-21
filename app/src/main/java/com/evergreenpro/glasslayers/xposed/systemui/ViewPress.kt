package com.evergreenpro.glasslayers.xposed.systemui

import android.content.res.ColorStateList
import com.evergreenpro.glasslayers.xposed.glass.PressSpring

internal class ViewPress(val active: () -> Boolean) {
    val spring = PressSpring()
    var x = 0f
    var y = 0f
    var stockRipple: ColorStateList? = null
}
