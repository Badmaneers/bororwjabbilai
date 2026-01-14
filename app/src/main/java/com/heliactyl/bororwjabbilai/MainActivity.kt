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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.ArrowUpward
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
                        favoritesRepository = FavoritesRepository(context),
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SongApp(
    songRepository: SongRepository,
    recentsRepository: RecentsRepository,
    favoritesRepository: FavoritesRepository,
    isDarkTheme: Boolean,
    onThemeCycle: () -> Unit
) {
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var recentIds by remember { mutableStateOf(recentsRepository.getRecentIds()) }
    var favoriteIds by remember { mutableStateOf(favoritesRepository.getFavoriteIds()) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    
    val homeListState = rememberLazyListState()
    val recentsListState = rememberLazyListState()
    val favoritesListState = rememberLazyListState()
    
    var filterChar by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var query by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    var hasSeenSwipeTutorial by remember { mutableStateOf(prefs.getBoolean("has_seen_swipe_tutorial", false)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            songs = songRepository.getSongs()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 2) {
            recentIds = recentsRepository.getRecentIds()
        } else if (pagerState.currentPage == 0) {
            favoriteIds = favoritesRepository.getFavoriteIds()
        }
    }
    
    fun toggleFavorite(song: Song) {
         favoritesRepository.toggleFavorite(song.id)
         favoriteIds = favoritesRepository.getFavoriteIds()
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Letter Filter", style = MaterialTheme.typography.titleMedium)
                    if (filterChar != null) {
                        TextButton(onClick = { 
                            filterChar = null
                            showFilterSheet = false
                        }) {
                            Text("Clear")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                val chars = ('A'..'Z').map { it.toString() } + "@"
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chars.forEach { char ->
                        FilterChip(
                            selected = filterChar == char,
                            onClick = { 
                                filterChar = if (filterChar == char) null else char
                                showFilterSheet = false
                            },
                            label = { Text(char) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (selectedSong != null) {
        SongDetailScreen(
            song = selectedSong!!,
            onBack = { selectedSong = null }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets.navigationBars,
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "Saved") },
                            label = { Text("Saved") },
                            selected = pagerState.currentPage == 0,
                            onClick = { 
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    Icons.Default.Home, 
                                    contentDescription = "Home",
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            if (dragAmount < -10) { // Drag up
                                                showFilterSheet = true
                                                if (!hasSeenSwipeTutorial) {
                                                    hasSeenSwipeTutorial = true
                                                    prefs.edit().putBoolean("has_seen_swipe_tutorial", true).apply()
                                                }
                                            }
                                        }
                                    }
                                )
                            },
                            label = { Text("Home") },
                            selected = pagerState.currentPage == 1,
                            onClick = { 
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                        )

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.History, contentDescription = "Recents") },
                            label = { Text("Recents") },
                            selected = pagerState.currentPage == 2,
                            onClick = { 
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
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
                    when (page) {
                        0 -> {
                            val favoriteSongs = remember(songs, favoriteIds) {
                                songs.filter { it.id in favoriteIds }
                            }
                             SongListScreen(
                                songs = favoriteSongs,
                                favoriteIds = favoriteIds,
                                onSongClick = { 
                                    selectedSong = it
                                    recentsRepository.addRecent(it.id)
                                    recentIds = recentsRepository.getRecentIds()
                                },
                                onFavoriteClick = { toggleFavorite(it) },
                                isDarkTheme = isDarkTheme,
                                onThemeCycle = onThemeCycle,
                                listState = favoritesListState
                            )
                        }
                        1 -> {
                            SongListScreen(
                                songs = songs,
                                favoriteIds = favoriteIds,
                                onSongClick = { 
                                    selectedSong = it
                                    recentsRepository.addRecent(it.id)
                                    recentIds = recentsRepository.getRecentIds()
                                },
                                onFavoriteClick = { toggleFavorite(it) },
                                isDarkTheme = isDarkTheme,
                                onThemeCycle = onThemeCycle,
                                listState = homeListState,
                                filterChar = filterChar,
                                query = query,
                                onQueryChange = { query = it },
                                active = active,
                                onActiveChange = { active = it }
                            )
                        }
                        2 -> {
                            val recentSongs = remember(songs, recentIds) {
                                val recentMap = recentIds.mapIndexed { index, id -> id to index }.toMap()
                                songs.filter { it.id in recentMap }
                                    .sortedBy { recentMap[it.id] }
                            }
                            SongListScreen(
                                songs = recentSongs,
                                favoriteIds = favoriteIds,
                                onSongClick = { 
                                    selectedSong = it
                                    recentsRepository.addRecent(it.id)
                                    recentIds = recentsRepository.getRecentIds()
                                },
                                onFavoriteClick = { toggleFavorite(it) },
                                isDarkTheme = isDarkTheme,
                                onThemeCycle = onThemeCycle,
                                listState = recentsListState
                            )
                        }
                    }
                }
            }

            if (!hasSeenSwipeTutorial && pagerState.currentPage == 1) {
                val infiniteTransition = rememberInfiniteTransition(label = "tutorial")
                val yOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -100f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "yOffset"
                )
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp) // Above Navigation Bar
                        .offset(y = yOffset.dp)
                        .alpha(alpha)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                "Swipe Up to Filter",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongListScreen(
    songs: List<Song>,
    favoriteIds: List<Int>,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    isDarkTheme: Boolean,
    onThemeCycle: () -> Unit,
    listState: LazyListState,
    filterChar: String? = null,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    active: Boolean = false,
    onActiveChange: (Boolean) -> Unit = {}
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    val filteredSongs = remember(query, songs, filterChar) {
        if (filterChar != null) {
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
            songs.filter {
                it.title.contains(q, ignoreCase = true) ||
                it.id.toString().contains(q) ||
                it.contentHtml.contains(q, ignoreCase = true) ||
                it.categoryChar.equals(q, ignoreCase = true)
            }.sortedWith(compareByDescending<Song> {
                it.title.startsWith(q, ignoreCase = true)
            }.thenByDescending {
                it.title.contains(q, ignoreCase = true)
            }.thenByDescending {
                it.categoryChar.equals(q, ignoreCase = true)
            }.thenByDescending {
                it.contentHtml.contains(q, ignoreCase = true)
            })
        }
    }

    fun getSnippet(contentHtml: String, query: String): String? {
        if (query.length < 2) return null
        val plainText = contentHtml.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
        val index = plainText.indexOf(query, ignoreCase = true)
        if (index == -1) return null
        
        val start = (index - 20).coerceAtLeast(0)
        val end = (index + query.length + 30).coerceAtMost(plainText.length)
        
        return (if (start > 0) "..." else "") + plainText.substring(start, end) + (if (end < plainText.length) "..." else "")
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
                    SongItem(
                        song = song, 
                        isFavorite = favoriteIds.contains(song.id),
                        snippet = if (query.isNotBlank()) getSnippet(song.contentHtml, query) else null,
                        onFavoriteClick = { onFavoriteClick(song) },
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }

        DockedSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { onActiveChange(false) },
            active = active,
            onActiveChange = onActiveChange,
            placeholder = { Text("Search song by number or title") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (active || query.isNotEmpty()) {
                    IconButton(onClick = {
                        if (query.isNotEmpty()) onQueryChange("") else onActiveChange(false)
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
                .padding(horizontal = 16.dp) 
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredSongs) { song ->
                    SongItem(
                        song = song,
                        isFavorite = favoriteIds.contains(song.id),
                        snippet = if (query.isNotBlank()) getSnippet(song.contentHtml, query) else null,
                        onFavoriteClick = { onFavoriteClick(song) },
                        onClick = {
                            onSongClick(song)
                            // Keep active state for return
                        }
                    )
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
fun SongItem(
    song: Song, 
    isFavorite: Boolean,
    snippet: String? = null,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!snippet.isNullOrBlank()) {
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Icon(
                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onFavoriteClick)
                    .padding(8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(song: Song, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    
    var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", false)) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("font_size", 18f)) }

    DisposableEffect(keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(song.title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (fontSize > 12f) {
                            fontSize -= 2f
                            prefs.edit().putFloat("font_size", fontSize).apply()
                        }
                    }) {
                        Text("A-", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { 
                        if (fontSize < 40f) {
                            fontSize += 2f
                            prefs.edit().putFloat("font_size", fontSize).apply()
                        }
                    }) {
                        Text("A+", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { 
                        keepScreenOn = !keepScreenOn
                        prefs.edit().putBoolean("keep_screen_on", keepScreenOn).apply()
                    }) {
                        Icon(
                            imageVector = if (keepScreenOn) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Keep Screen On",
                            tint = if (keepScreenOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        // Initial setup
                    }
                },
                update = { textView ->
                    textView.setTextColor(textColor)
                    textView.textSize = fontSize
                    textView.text = HtmlCompat.fromHtml(song.contentHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
            )
        }
    }
}
