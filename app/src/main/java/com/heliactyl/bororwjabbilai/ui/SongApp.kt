package com.heliactyl.bororwjabbilai.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import com.heliactyl.bororwjabbilai.BuildConfig
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.heliactyl.bororwjabbilai.FavoritesRepository
import com.heliactyl.bororwjabbilai.RecentsRepository
import com.heliactyl.bororwjabbilai.Song
import com.heliactyl.bororwjabbilai.SongRepository
import com.heliactyl.bororwjabbilai.ui.Occasion
import com.heliactyl.bororwjabbilai.ui.components.SocialRow
import com.heliactyl.bororwjabbilai.ui.components.SongItem
import com.heliactyl.bororwjabbilai.ui.components.bouncyClick
import com.heliactyl.bororwjabbilai.ui.components.liquidGlass
import com.heliactyl.bororwjabbilai.ui.occasionFilters
import com.heliactyl.bororwjabbilai.ui.screens.SongDetailScreen
import com.heliactyl.bororwjabbilai.ui.screens.SongListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SongApp(
    songRepository: SongRepository,
    recentsRepository: RecentsRepository,
    favoritesRepository: FavoritesRepository,
    isDarkTheme: Boolean,
    onThemeCycle: (Offset) -> Unit
) {
    val context = LocalContext.current
    val customSongRepository = remember { com.heliactyl.bororwjabbilai.CustomSongRepository(context) }
    val updateManager = remember { com.heliactyl.bororwjabbilai.UpdateManager(context) }
    
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var customSongs by remember { mutableStateOf(customSongRepository.getCustomSongs()) }
    
    val allSongs by remember(songs, customSongs) {
        derivedStateOf { songs + customSongs }
    }
    
    var recentIds by remember { mutableStateOf(recentsRepository.getRecentIds()) }
    var favoriteIds by remember { mutableStateOf(favoritesRepository.getFavoriteIds()) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var editingSong by remember { mutableStateOf<Song?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importCode by remember { mutableStateOf("") }
    
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    
    val homeListState = rememberLazyListState()
    val recentsListState = rememberLazyListState()
    val favoritesListState = rememberLazyListState()
    
    var filterChar by remember { mutableStateOf<String?>(null) }
    var filterOccasion by remember { mutableStateOf<Occasion?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var updateRelease by remember { mutableStateOf<com.heliactyl.bororwjabbilai.GitHubRelease?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(-1f) }

    var query by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    var hasSeenSwipeTutorial by remember { mutableStateOf(prefs.getBoolean("has_seen_swipe_tutorial", false)) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(Unit) {
        updateManager.clearCache() // Cleanup old APKs on startup
        withContext(Dispatchers.IO) {
            songs = songRepository.getSongs()
        }
        updateRelease = updateManager.checkForUpdate()
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
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showFilterSheet = false },
            containerColor = Color.Transparent,
            dragHandle = {
                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(32.dp, 4.dp)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            val filterPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .liquidGlass(cornerRadius = 32)
                    .padding(bottom = 32.dp)
            ) {
                TabRow(
                    selectedTabIndex = filterPagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(48.dp)
                        .liquidGlass(
                            cornerRadius = 24,
                            color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)
                        ),
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            // Calculate a continuous page value from 0 to N-1
                            val continuousPage = filterPagerState.currentPage + filterPagerState.currentPageOffsetFraction
                            val index = continuousPage.toInt().coerceIn(0, tabPositions.size - 2)
                            val fraction = continuousPage - index
                            
                            val currentTab = tabPositions[index]
                            val nextTab = tabPositions[index + 1]
                            
                            // Smoothly interpolate position and width
                            val lerpLeft = currentTab.left + (nextTab.left - currentTab.left) * fraction
                            val lerpWidth = currentTab.width + (nextTab.width - currentTab.width) * fraction
                            
                            // Adjust these values to make the pill smaller
                            val horizontalPadding = 32.dp
                            val verticalPadding = 10.dp
                            val bubbleWidth = lerpWidth - (horizontalPadding * 2)
                            
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .width(bubbleWidth)
                                    .offset {
                                        IntOffset(x = (lerpLeft + horizontalPadding).roundToPx(), y = 0)
                                    }
                                    .padding(vertical = verticalPadding)
                                    .zIndex(0f)
                                    .liquidGlass(
                                        cornerRadius = 20,
                                        color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) 
                                            Color.White.copy(alpha = 0.12f) 
                                        else 
                                            Color.White.copy(alpha = 0.85f),
                                        blurRadius = 20f
                                    )
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.4f),
                                                    Color.White.copy(alpha = 0.05f),
                                                    Color.Transparent
                                                )
                                            ),
                                            RoundedCornerShape(20.dp)
                                        )
                                )
                            }
                        }
                    }
                ) {
                    androidx.compose.material3.Tab(
                        selected = filterPagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { filterPagerState.animateScrollToPage(0) } },
                        modifier = Modifier.zIndex(1f),
                        text = { Text("Letter", fontWeight = if (filterPagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    androidx.compose.material3.Tab(
                        selected = filterPagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { filterPagerState.animateScrollToPage(1) } },
                        modifier = Modifier.zIndex(1f),
                        text = { Text("Category", fontWeight = if (filterPagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                HorizontalPager(
                    state = filterPagerState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).animateContentSize()
                ) { page ->
                    when (page) {
                        0 -> {
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
                                    Text(
                                        text = "Select Letter Filter",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
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
                                        Box(
                                            modifier = Modifier
                                                .liquidGlass(
                                                    color = if (filterChar == char) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                                    cornerRadius = 12
                                                )
                                                .bouncyClick {
                                                    filterChar = if (filterChar == char) null else char
                                                    filterOccasion = null
                                                    showFilterSheet = false
                                                }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = char,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (filterChar == char) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
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
                                    Text(
                                        text = "Select Category",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (filterOccasion != null) {
                                        TextButton(onClick = { 
                                            filterOccasion = null
                                            showFilterSheet = false
                                        }) {
                                            Text("Clear")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                androidx.compose.foundation.lazy.LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(occasionFilters.size) { index ->
                                        val occasion = occasionFilters[index]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .liquidGlass(
                                                    color = if (filterOccasion == occasion) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                                    cornerRadius = 16
                                                )
                                                .bouncyClick {
                                                    filterOccasion = if (filterOccasion == occasion) null else occasion
                                                    filterChar = null
                                                    showFilterSheet = false
                                                }
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = occasion.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (filterOccasion == occasion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showInfoDialog) {
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .components {
                    add(SvgDecoder.Factory())
                }
                .build()
        }
        Dialog(
            onDismissRequest = { showInfoDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
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
                            tint = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White else Color.Black,
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
                            tint = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White else Color.Black,
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
    }

    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = 24)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Import Custom Song",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Paste the song code shared by another user to import it with its original formatting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                OutlinedTextField(
                    value = importCode,
                    onValueChange = { importCode = it },
                    label = { Text("Song Code") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showImportDialog = false; importCode = "" }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val imported = customSongRepository.importSongFromCode(importCode)
                            if (imported != null) {
                                customSongRepository.saveSong(imported)
                                customSongs = customSongRepository.getCustomSongs()
                                showImportDialog = false
                                importCode = ""
                            }
                        },
                        enabled = importCode.isNotBlank()
                    ) {
                        Text("Import")
                    }
                }
            }
        }
    }

    updateRelease?.let { release ->
        Dialog(
            onDismissRequest = { if (downloadProgress < 0f) updateRelease = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = downloadProgress < 0f,
                dismissOnClickOutside = downloadProgress < 0f
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 24)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (downloadProgress >= 0f) "Downloading Update..." else "New Update Available!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (downloadProgress >= 0f) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "Version ${release.tagName} is now available. Here's what's new:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            val scroll = rememberScrollState()
                            Text(
                                text = release.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.verticalScroll(scroll)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { updateRelease = null }) {
                                Text("Later")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                                    if (apkAsset != null) {
                                        coroutineScope.launch {
                                            downloadProgress = 0f
                                            updateManager.downloadApk(apkAsset.downloadUrl).collect { progress ->
                                                downloadProgress = progress
                                            }
                                            downloadProgress = 1f
                                            updateManager.installApk()
                                            // Dialog will remain until user installs or app restarts
                                        }
                                    } else {
                                        // Fallback to browser if no APK found in assets
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                                        context.startActivity(intent)
                                        updateRelease = null
                                    }
                                }
                            ) {
                                Text("Update Now")
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Detail State
    val detailSong = remember { mutableStateOf<Song?>(null) }
    LaunchedEffect(selectedSong) {
        if (selectedSong != null) detailSong.value = selectedSong
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val baseBlur by animateDpAsState(
            targetValue = if (showFilterSheet || showInfoDialog || updateRelease != null) 10.dp else 0.dp,
            animationSpec = tween(durationMillis = 300),
            label = "baseBlur"
        )
        
        val blurFraction by remember(sheetState, showInfoDialog, updateRelease) {
            derivedStateOf {
                if (showInfoDialog || updateRelease != null) return@derivedStateOf 1f
                try {
                    val offset = sheetState.requireOffset()
                    val fraction = 1f - (offset / screenHeightPx)
                    fraction.coerceIn(0f, 1f)
                } catch (e: Exception) {
                    1f
                }
            }
        }
        
        val blurRadius = baseBlur * blurFraction
        
        // 1. MAIN APP CONTENT (Always in composition for interactive transitions)
        Box(modifier = Modifier.fillMaxSize().blur(blurRadius)) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { innerPadding ->
                val topBarHeight = 140.dp 
                val bottomBarHeight = 110.dp
                
                Box(Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val screenPadding = PaddingValues(
                            top = topBarHeight,
                            bottom = bottomBarHeight
                        )
                        
                        when (page) {
                            0 -> {
                                val favoriteSongs = remember(allSongs, favoriteIds) {
                                    allSongs.filter { it.id in favoriteIds }
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
                                    listState = favoritesListState,
                                    headerTitle = "Saved",
                                    headerSubtitle = "Your ${favoriteSongs.size} saved favorites",
                                    contentPadding = screenPadding
                                )
                            }
                            1 -> {
                                val homeFilteredSongs = remember(allSongs, filterChar, filterOccasion) {
                                    val currentOccasion = filterOccasion
                                    val currentChar = filterChar
                                    if (currentOccasion != null) {
                                        allSongs.filter { it.id in currentOccasion.range }
                                    } else if (currentChar != null) {
                                        if (currentChar == "@") {
                                            allSongs.filter { !it.categoryChar[0].isLetter() }
                                        } else {
                                            allSongs.filter { it.categoryChar.equals(currentChar, ignoreCase = true) }
                                        }
                                    } else {
                                        allSongs
                                    }
                                }
                                SongListScreen(
                                    songs = homeFilteredSongs,
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
                                    filterOccasion = filterOccasion,
                                    headerTitle = "Discover",
                                    headerSubtitle = "${homeFilteredSongs.size} sacred songs",
                                    contentPadding = screenPadding
                                )
                            }
                            2 -> {
                                val recentSongs = remember(allSongs, recentIds) {
                                    val recentMap = recentIds.mapIndexed { index, id -> id to index }.toMap()
                                    allSongs.filter { it.id in recentMap }
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
                                    listState = recentsListState,
                                    headerTitle = "Recents",
                                    headerSubtitle = "Your ${recentSongs.size} recent hymns",
                                    contentPadding = screenPadding
                                )
                            }
                        }
                    }

                    // Floating Top Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .liquidGlass(cornerRadius = 28, blurRadius = 40f)
                        )
                        androidx.compose.material3.DockedSearchBar(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { active = false },
                            active = active,
                            onActiveChange = { active = it },
                            placeholder = { Text("Search song...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            colors = androidx.compose.material3.SearchBarDefaults.colors(
                                containerColor = Color.Transparent,
                            ),
                            trailingIcon = {
                                if (active || query.isNotEmpty()) {
                                    IconButton(onClick = {
                                        if (query.isNotEmpty()) query = "" else active = false
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                } else {
                                    Row {
                                        if (BuildConfig.DEBUG) {
                                            IconButton(onClick = {
                                                coroutineScope.launch {
                                                    val url = java.net.URL("https://api.github.com/repos/Badmaneers/bororwjabbilai/releases/latest")
                                                    var errorDetail = ""
                                                    val result = withContext(Dispatchers.IO) {
                                                        try {
                                                            val conn = url.openConnection() as java.net.HttpURLConnection
                                                            conn.setRequestProperty("User-Agent", "BoroRwjabBilai-App")
                                                            if (conn.responseCode == 200) {
                                                                val json = conn.inputStream.bufferedReader().use { it.readText() }
                                                                com.google.gson.Gson().fromJson(json, com.heliactyl.bororwjabbilai.GitHubRelease::class.java)
                                                            } else {
                                                                errorDetail = "HTTP ${conn.responseCode}"
                                                                null
                                                            }
                                                        } catch (e: Exception) {
                                                            errorDetail = e.localizedMessage ?: "Unknown error"
                                                            null
                                                        }
                                                    }
                                                    if (result != null) updateRelease = result
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.BugReport,
                                                    contentDescription = "Test Update",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        IconButton(onClick = { showEditor = true }) {
                                            Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { showImportDialog = true }) {
                                            Icon(Icons.Default.VerticalAlignBottom, "Import", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        var themeButtonCenter by remember { mutableStateOf(Offset.Zero) }
                                        IconButton(
                                            modifier = Modifier.onGloballyPositioned { 
                                                val rootPos = it.positionInRoot()
                                                val size = it.size
                                                themeButtonCenter = Offset(rootPos.x + size.width / 2f, rootPos.y + size.height / 2f)
                                            },
                                            onClick = { onThemeCycle(themeButtonCenter) }
                                        ) {
                                            val icon = if (isDarkTheme) Icons.Default.NightsStay else Icons.Default.WbSunny
                                            Icon(icon, "Theme", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { showInfoDialog = true }) {
                                            Icon(Icons.Default.Info, "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filteredSongs = remember(query, allSongs) {
                                if (query.isBlank()) allSongs
                                else {
                                    val q = query.trim()
                                    allSongs.filter { song ->
                                        val lyricsText = song.lyrics.flatMap { it.lines }.joinToString(" ")
                                        song.title.contains(q, ignoreCase = true) ||
                                        song.id.toString().contains(q) ||
                                        lyricsText.contains(q, ignoreCase = true)
                                    }
                                }
                            }
                            androidx.compose.foundation.lazy.LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredSongs) { song ->
                                    SongItem(
                                        song = song,
                                        isFavorite = favoriteIds.contains(song.id),
                                        onFavoriteClick = { toggleFavorite(song) },
                                        onClick = {
                                            selectedSong = song
                                            recentsRepository.addRecent(song.id)
                                            recentIds = recentsRepository.getRecentIds()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Floating Bottom Bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .liquidGlass(cornerRadius = 32, blurRadius = 40f)
                        )
                        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Favorite, "Saved") },
                                label = { Text("Saved") },
                                selected = pagerState.currentPage == 0,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                            )
                            NavigationBarItem(
                                icon = { 
                                    Icon(Icons.Default.Home, "Home", modifier = Modifier.pointerInput(Unit) {
                                        detectVerticalDragGestures { _, dragAmount ->
                                            if (dragAmount < -10) { 
                                                showFilterSheet = true
                                                if (!hasSeenSwipeTutorial) {
                                                    hasSeenSwipeTutorial = true
                                                    prefs.edit().putBoolean("has_seen_swipe_tutorial", true).apply()
                                                }
                                            }
                                        }
                                    })
                                },
                                label = { Text("Home") },
                                selected = pagerState.currentPage == 1,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.History, "Recents") },
                                label = { Text("Recents") },
                                selected = pagerState.currentPage == 2,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                            )
                        }
                    }
                }
            }
        }
        
        // 2. SONG DETAIL OVERLAY (interactive)
        AnimatedVisibility(
            visible = selectedSong != null,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut()
        ) {
            detailSong.value?.let { song ->
                SongDetailScreen(
                    song = song,
                    onBack = { selectedSong = null },
                    onEdit = { songToEdit ->
                        editingSong = songToEdit
                        showEditor = true
                        selectedSong = null
                    }
                )
            }
        }

        // 3. EDITOR OVERLAY
        AnimatedVisibility(
            visible = showEditor,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            com.heliactyl.bororwjabbilai.ui.screens.CustomSongEditorScreen(
                initialSong = editingSong,
                onSave = { newSong ->
                    val songToSave = if (newSong.id == -1) {
                        newSong.copy(id = customSongRepository.generateNewId())
                    } else newSong
                    customSongRepository.saveSong(songToSave)
                    customSongs = customSongRepository.getCustomSongs()
                    showEditor = false
                    editingSong = null
                },
                onCancel = {
                    showEditor = false
                    editingSong = null
                }
            )
        }

        if (!hasSeenSwipeTutorial && pagerState.currentPage == 1 && selectedSong == null) {
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
                    .padding(bottom = 140.dp)
                    .offset(y = yOffset.dp)
                    .alpha(alpha)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 4.dp) {
                        Text("Swipe Up to Filter", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(Icons.Default.TouchApp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}
