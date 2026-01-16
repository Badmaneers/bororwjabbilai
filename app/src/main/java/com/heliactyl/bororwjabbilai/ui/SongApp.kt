package com.heliactyl.bororwjabbilai.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.heliactyl.bororwjabbilai.FavoritesRepository
import com.heliactyl.bororwjabbilai.RecentsRepository
import com.heliactyl.bororwjabbilai.Song
import com.heliactyl.bororwjabbilai.SongRepository
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

    AnimatedContent(
        targetState = selectedSong,
        label = "SongDetailTransition",
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(
                    slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()).togetherWith(
                    slideOutHorizontally { it } + fadeOut())
            }
        }
    ) { targetState ->
        if (targetState != null) {
            SongDetailScreen(
                song = targetState,
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
}
