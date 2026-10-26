package com.heliactyl.bororwjabbilai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.heliactyl.bororwjabbilai.Song
import com.heliactyl.bororwjabbilai.ui.paper.PaperSurfaceStyle
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperCard
import com.heliactyl.bororwjabbilai.ui.theme.paper.PaperTheme

@Composable
fun SongItem(
    song: Song, 
    isFavorite: Boolean,
    snippet: String? = null,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = PaperTheme.colors
    val typography = PaperTheme.typography

    PaperCard(
        onClick = onClick,
        style = PaperSurfaceStyle.RULED,
        modifier = Modifier.fillMaxWidth(),
        shape = PaperTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${song.id}.", 
                style = typography.titleMedium,
                color = colors.accentInk,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = typography.bodyLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (!snippet.isNullOrBlank()) {
                    Text(
                        text = snippet,
                        style = typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontStyle = FontStyle.Italic,
                        color = colors.textSecondary
                    )
                }
            }
            Icon(
                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = colors.accentInk,
                modifier = Modifier
                    .bouncyClick(onClick = onFavoriteClick)
                    .padding(8.dp)
            )
        }
    }
}
