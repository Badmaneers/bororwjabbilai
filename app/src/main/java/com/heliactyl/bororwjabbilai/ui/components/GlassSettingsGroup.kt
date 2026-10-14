package com.heliactyl.bororwjabbilai.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heliactyl.bororwjabbilai.ui.glass.GlassShapes

/**
 * Echo DESIGN.md §3 grouped-list (iOS Settings style).
 * Single 24dp card for one item, segmented 24/4dp corners for groups,
 * flat 0dp elevation, [surfaceContainerHighest] container.
 */
@Composable
fun GlassSettingsGroup(
    title: String? = null,
    items: List<GlassSettingsItem>,
) {
    Column(Modifier.fillMaxWidth()) {
        title?.let {
            androidx.compose.material3.Text(
                text = it.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp, start = 8.dp)
            )
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { index, item ->
                val shape = when {
                    items.size == 1 -> GlassShapes.Card
                    index == 0 -> GlassShapes.GroupTop
                    index == items.size - 1 -> GlassShapes.GroupBottom
                    else -> GlassShapes.GroupMiddle
                }
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = item.onClick != null, onClick = { item.onClick?.invoke() })
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            ProvideTextStyle(
                                MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            ) { item.title() }
                            item.description?.let { desc ->
                                Spacer(Modifier.height(2.dp))
                                ProvideTextStyle(
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) { desc() }
                            }
                        }
                        item.trailing?.let {
                            Spacer(Modifier.width(8.dp))
                            it()
                        }
                    }
                }
            }
        }
    }
}

data class GlassSettingsItem(
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
)
