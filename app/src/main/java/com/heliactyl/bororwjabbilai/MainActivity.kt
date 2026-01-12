package com.heliactyl.bororwjabbilai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.heliactyl.bororwjabbilai.ui.theme.BoroRwjabBilaiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkTheme by remember { mutableStateOf(systemDark) }

            BoroRwjabBilaiTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SongApp(
                        songRepository = SongRepository(this),
                        isDarkTheme = darkTheme,
                        onThemeToggle = { darkTheme = !darkTheme }
                    )
                }
            }
        }
    }
}

@Composable
fun SongApp(
    songRepository: SongRepository,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            songs = songRepository.getSongs()
        }
    }

    if (selectedSong != null) {
        SongDetailScreen(
            song = selectedSong!!,
            onBack = { selectedSong = null }
        )
    } else {
        SongListScreen(
            songs = songs,
            onSongClick = { selectedSong = it },
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val filteredSongs = remember(query, songs) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.id.toString().contains(query)
        }
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Developer Info") },
            text = {
                val context = LocalContext.current
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SocialRow(
                        text = "Instagram",
                        handle = "@heliactyl",
                        iconModel = "file:///android_asset/instagram.svg",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/heliactyl"))
                            context.startActivity(intent)
                        }
                    )
                    SocialRow(
                        text = "Telegram",
                        handle = "@dumbdragon",
                        iconModel = "file:///android_asset/telegram.svg",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/dumbdragon"))
                            context.startActivity(intent)
                        }
                    )
                    SocialRow(
                        text = "GitHub",
                        handle = "Badmaneers",
                        iconModel = "file:///android_asset/github.svg",
                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Badmaneers"))
                            context.startActivity(intent)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        if (!active) {
            LazyColumn(
                contentPadding = PaddingValues(top = statusBarHeight + 72.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSongs) { song ->
                    SongItem(song = song, onClick = { onSongClick(song) })
                }
            }
        }

        SearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = { active = false },
            active = active,
            onActiveChange = { active = it },
            placeholder = { Text("Search song by number or title") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (active || query.isNotEmpty()) {
                    IconButton(onClick = {
                        if (query.isNotEmpty()) query = "" else active = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                } else {
                    Row {
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Developer Info",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = if (active) 0.dp else 16.dp) 
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredSongs) { song ->
                    SongItem(song = song, onClick = {
                        onSongClick(song)
                        active = false
                    })
                }
            }
        }
    }
}

@Composable
fun SocialRow(
    text: String, 
    handle: String, 
    iconModel: Any, 
    tint: Color? = null, 
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(iconModel)
                .build(),
            imageLoader = imageLoader,
            contentDescription = text,
            modifier = Modifier.size(24.dp),
            colorFilter = tint?.let { ColorFilter.tint(it) }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = handle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${song.id}.", 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(song: Song, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        textSize = 18f
                    }
                },
                update = { textView ->
                    textView.setTextColor(textColor)
                    textView.text = HtmlCompat.fromHtml(song.contentHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
            )
        }
    }
}
