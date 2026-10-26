package com.heliactyl.bororwjabbilai.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heliactyl.bororwjabbilai.Song
import com.heliactyl.bororwjabbilai.ui.Occasion
import com.heliactyl.bororwjabbilai.ui.components.SongItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongListScreen(
    songs: List<Song>,
    favoriteIds: List<Int>,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    @Suppress("UNUSED_PARAMETER") isDarkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onThemeCycle: (Offset) -> Unit = {},
    listState: LazyListState,
    filterChar: String? = null,
    filterOccasion: Occasion? = null,
    query: String = "",
    @Suppress("UNUSED_PARAMETER") onQueryChange: (String) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") active: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onActiveChange: (Boolean) -> Unit = {},
    headerTitle: String? = null,
    headerSubtitle: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val favoriteSet = remember(favoriteIds) { favoriteIds.toSet() }

    val filteredSongs = remember(query, songs, filterChar, filterOccasion) {
        if (filterOccasion != null) {
            songs.filter { it.id in filterOccasion.range }
        } else if (filterChar != null) {
            val q = filterChar
            if (q == "@") {
                songs.filter { !it.categoryChar[0].isLetter() }
            } else {
                songs.filter {
                    it.categoryChar.equals(q, ignoreCase = true)
                }
            }
        } else if (query.isBlank()) {
            songs
        } else {
            val q = query.trim().lowercase()
            songs.asSequence().filter { song ->
                song.searchTitleLower.contains(q) ||
                song.searchIdString.contains(q) ||
                song.searchLyricsText.contains(q, ignoreCase = true) ||
                song.categoryChar.equals(q, ignoreCase = true)
            }.sortedWith(
                compareByDescending<Song> {
                    it.searchTitleLower.startsWith(q)
                }.thenByDescending {
                    it.searchTitleLower.contains(q)
                }.thenByDescending {
                    it.categoryChar.equals(q, ignoreCase = true)
                }.thenByDescending {
                    it.searchLyricsText.contains(q, ignoreCase = true)
                }
            ).toList()
        }
    }

    fun getSnippet(song: Song, queryStr: String): String? {
        if (queryStr.length < 2) return null
        val plainText = song.searchLyricsText
        val index = plainText.indexOf(queryStr, ignoreCase = true)
        if (index == -1) return null
        
        val start = (index - 20).coerceAtLeast(0)
        val end = (index + queryStr.length + 30).coerceAtMost(plainText.length)
        
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < plainText.length) "..." else ""
        return "$prefix${plainText.substring(start, end)}$suffix"
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp, 
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Reachability Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = when {
                            filterOccasion != null -> filterOccasion.name
                            filterChar != null -> "Letter: $filterChar"
                            else -> headerTitle ?: "Discover"
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = headerSubtitle ?: "Explore ${filteredSongs.size} sacred songs",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            items(filteredSongs, key = { it.id }) { song ->
                SongItem(
                    song = song, 
                    isFavorite = song.id in favoriteSet,
                    snippet = if (query.isNotBlank()) getSnippet(song, query) else null,
                    onFavoriteClick = { onFavoriteClick(song) },
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}
