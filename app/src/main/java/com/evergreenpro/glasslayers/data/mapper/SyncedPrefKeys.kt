package com.evergreenpro.glasslayers.data.mapper

import com.evergreenpro.glasslayers.data.source.PrefKeys
import com.evergreenpro.glasslayers.domain.model.GlassLevel
import com.evergreenpro.glasslayers.domain.model.GlassSurface
import com.evergreenpro.glasslayers.domain.model.GlassToggle
import com.evergreenpro.glasslayers.domain.model.StyleParam

object SyncedPrefKeys {

    val all: Set<String> by lazy {
        buildSet {
            GlassToggle.entries.forEach { add(GlassPrefMapper.key(it)) }
            GlassLevel.entries.forEach { add(GlassPrefMapper.key(it)) }
            add(PrefKeys.PERFORMANCE_MODE)
            add(PrefKeys.BATTERY_SAVER_ACTION)
            add(PrefKeys.THERMAL_ACTION)
            add(PrefKeys.BATTERY_SAVER_BACKGROUND)
            StyleParam.entries.forEach { param ->
                add(GlassPrefMapper.key(param))
                GlassSurface.entries.forEach { add(GlassPrefMapper.key(it, param)) }
            }
        }
    }
}
