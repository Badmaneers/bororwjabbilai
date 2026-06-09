package com.heliactyl.bororwjabbilai.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.heliactyl.bororwjabbilai.Song
import com.heliactyl.bororwjabbilai.ui.Occasion
import com.heliactyl.bororwjabbilai.LyricSection
import com.heliactyl.bororwjabbilai.ui.components.SocialRow
import com.heliactyl.bororwjabbilai.ui.components.SongItem
import com.heliactyl.bororwjabbilai.ui.components.liquidGlass
import com.heliactyl.bororwjabbilai.ui.components.bouncyClick

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongListScreen(
    songs: List<Song>,
    favoriteIds: List<Int>,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    isDarkTheme: Boolean,
    onThemeCycle: (Offset) -> Unit,
    listState: LazyListState,
    filterChar: String? = null,
    filterOccasion: Occasion? = null,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    active: Boolean = false,
    onActiveChange: (Boolean) -> Unit = {},
    headerTitle: String? = null,
    headerSubtitle: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var showInfoDialog by remember { mutableStateOf(false) }

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
            val q = query.trim()
            songs.filter { song ->
                val lyricsText = song.lyrics.flatMap { it.lines }.joinToString(" ")
                song.title.contains(q, ignoreCase = true) ||
                song.id.toString().contains(q) ||
                lyricsText.contains(q, ignoreCase = true) ||
                song.categoryChar.equals(q, ignoreCase = true)
            }.sortedWith(compareByDescending<Song> {
                it.title.startsWith(q, ignoreCase = true)
            }.thenByDescending {
                it.title.contains(q, ignoreCase = true)
            }.thenByDescending {
                it.categoryChar.equals(q, ignoreCase = true)
            }.thenByDescending {
                val lyricsText = it.lyrics.flatMap { sec -> sec.lines }.joinToString(" ")
                lyricsText.contains(q, ignoreCase = true)
            })
        }
    }

    fun getSnippet(lyrics: List<LyricSection>, query: String): String? {
        if (query.length < 2) return null
        val plainText = lyrics.flatMap { it.lines }.joinToString(" ")
        val index = plainText.indexOf(query, ignoreCase = true)
        if (index == -1) return null
        
        val start = (index - 20).coerceAtLeast(0)
        val end = (index + query.length + 30).coerceAtMost(plainText.length)
        
        return (if (start > 0) "..." else "") + plainText.substring(start, end) + (if (end < plainText.length) "..." else "")
    }

    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    if (showInfoDialog) {
        Dialog(onDismissRequest = { showInfoDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = 32)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Developer Info",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialRow(
                        text = "Instagram",
                        handle = "@heliactyl",
                        iconModel = "file:///android_asset/instagram.svg",
                        imageLoader = imageLoader,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/heliactyl"))
                            context.startActivity(intent)
                        }
                    )
                    SocialRow(
                        text = "Telegram",
                        handle = "@dumbdragon",
                        iconModel = "file:///android_asset/telegram.svg",
                        imageLoader = imageLoader,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/dumbdragon"))
                            context.startActivity(intent)
                        }
                    )
                    SocialRow(
                        text = "GitHub",
                        handle = "Badmaneers",
                        iconModel = "file:///android_asset/github.svg",
                        imageLoader = imageLoader,
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Badmaneers"))
                            context.startActivity(intent)
                        }
                    )
                    SocialRow(
                        text = "Email",
                        handle = "dukebraham24@gmail.com",
                        iconVector = Icons.Default.Email,
                        imageLoader = imageLoader,
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:dukebraham24@gmail.com")
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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

            items(filteredSongs) { song ->
                SongItem(
                    song = song, 
                    isFavorite = favoriteIds.contains(song.id),
                    snippet = if (query.isNotBlank()) getSnippet(song.lyrics, query) else null,
                    onFavoriteClick = { onFavoriteClick(song) },
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}
