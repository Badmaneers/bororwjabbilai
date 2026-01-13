package com.heliactyl.bororwjabbilai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
            
            // 0: System, 1: Light, 2: Dark
            var themeMode by remember(systemDark) { 
                val lastSystemDark = prefs.getBoolean("last_system_dark", systemDark)
                val initialMode = if (lastSystemDark != systemDark) {
                    prefs.edit()
                        .putInt("theme_mode", 0)
                        .putBoolean("last_system_dark", systemDark)
                        .apply()
                    0
                } else {
                    if (!prefs.contains("last_system_dark")) {
                        prefs.edit().putBoolean("last_system_dark", systemDark).apply()
                    }
                    prefs.getInt("theme_mode", 0)
                }
                mutableIntStateOf(initialMode)
            }

            val useDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemDark
            }

            BoroRwjabBilaiTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SongApp(
                        songRepository = SongRepository(context),
                        recentsRepository = RecentsRepository(context),
                        isDarkTheme = useDarkTheme,
                        onThemeCycle = {
                            val newMode = when (themeMode) {
                                0 -> if (systemDark) 1 else 2
                                1 -> 2
                                else -> 1
                            }
                            themeMode = newMode
                            prefs.edit().putInt("theme_mode", newMode).apply()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongApp(
    songRepository: SongRepository,
    recentsRepository: RecentsRepository,
    isDarkTheme: Boolean,
    onThemeCycle: () -> Unit
) {
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var recentIds by remember { mutableStateOf(recentsRepository.getRecentIds()) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    
    val homeListState = rememberLazyListState()
    val recentsListState = rememberLazyListState()
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            songs = songRepository.getSongs()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            recentIds = recentsRepository.getRecentIds()
        }
    }

    if (selectedSong != null) {
        SongDetailScreen(
            song = selectedSong!!,
            onBack = { selectedSong = null }
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = pagerState.currentPage == 0,
                        onClick = { 
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "Recents") },
                        label = { Text("Recents") },
                        selected = pagerState.currentPage == 1,
                        onClick = { 
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                if (page == 0) {
                    SongListScreen(
                        songs = songs,
                        onSongClick = { 
                            selectedSong = it
                            recentsRepository.addRecent(it.id)
                            recentIds = recentsRepository.getRecentIds()
                        },
                        isDarkTheme = isDarkTheme,
                        onThemeCycle = onThemeCycle,
                        listState = homeListState
                    )
                } else {
                    val recentSongs = remember(songs, recentIds) {
                        val recentMap = recentIds.mapIndexed { index, id -> id to index }.toMap()
                        songs.filter { it.id in recentMap }
                            .sortedBy { recentMap[it.id] }
                    }
                    SongListScreen(
                        songs = recentSongs,
                        onSongClick = { 
                            selectedSong = it
                            recentsRepository.addRecent(it.id)
                            recentIds = recentsRepository.getRecentIds()
                        },
                        isDarkTheme = isDarkTheme,
                        onThemeCycle = onThemeCycle,
                        listState = recentsListState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    isDarkTheme: Boolean,
    onThemeCycle: () -> Unit,
    listState: LazyListState
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
    
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Developer Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                state = listState,
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
                        IconButton(onClick = onThemeCycle) {
                            val icon = if (isDarkTheme) Icons.Default.NightsStay else Icons.Default.WbSunny
                            Icon(
                                imageVector = icon,
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
    imageLoader: ImageLoader,
    iconModel: Any? = null,
    iconVector: ImageVector? = null,
    tint: Color? = null, 
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = text,
                tint = tint ?: LocalContentColor.current,
                modifier = Modifier.size(24.dp)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconModel)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                colorFilter = tint?.let { ColorFilter.tint(it) }
            )
        }
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
