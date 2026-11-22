package com.evergreenpro.glasslayers.ui.settings

import androidx.annotation.StringRes
import com.evergreenpro.glasslayers.R
import com.evergreenpro.glasslayers.domain.model.GlassToggle

@StringRes
internal fun optionTitle(option: GlassToggle): Int = when (option) {
    GlassToggle.SWITCH_PRESS -> R.string.option_switch_press
    GlassToggle.BUTTON_PRESS -> R.string.option_button_press
    GlassToggle.CHARGING_RIPPLE -> R.string.option_charging_ripple
    else -> R.string.page_enabled
}

@StringRes
internal fun optionSummary(option: GlassToggle): Int = when (option) {
    GlassToggle.SWITCH_PRESS -> R.string.option_switch_press_summary
    GlassToggle.BUTTON_PRESS -> R.string.option_button_press_summary
    GlassToggle.CHARGING_RIPPLE -> R.string.option_charging_ripple_summary
    else -> R.string.page_enabled
}
