package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runway.android.ui.theme.LocalIsDarkTheme
import com.runway.android.ui.theme.OutlineVariantDark
import com.runway.android.ui.theme.SurfaceContainerDark

@Composable
fun StatsSummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (isDark) SurfaceContainerDark else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isDark) OutlineVariantDark else MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
