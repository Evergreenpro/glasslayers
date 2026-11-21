package com.evergreenpro.glasslayers.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.evergreenpro.glasslayers.ui.components.LocalBackdrop
import com.evergreenpro.glasslayers.ui.components.liquid.LiquidToggle
import com.evergreenpro.glasslayers.ui.components.pressable

@Composable
internal fun ToggleRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(Role.Switch) { onCheckedChange(!checked) }
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowText(title, summary, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        LiquidToggle(checked, onCheckedChange, LocalBackdrop.current)
    }
}
