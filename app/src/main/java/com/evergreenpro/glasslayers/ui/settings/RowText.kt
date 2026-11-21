package com.evergreenpro.glasslayers.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evergreenpro.glasslayers.ui.theme.OnInk
import com.evergreenpro.glasslayers.ui.theme.OnInkMuted

@Composable
internal fun RowText(title: String, summary: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = OnInk)
        Text(summary, style = MaterialTheme.typography.bodySmall, color = OnInkMuted)
    }
}
