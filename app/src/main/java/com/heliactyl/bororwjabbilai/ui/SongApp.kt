package com.heliactyl.bororwjabbilai.ui

import android.content.Intent
import android.net.Uri
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.heliactyl.bororwjabbilai.ui.paper.PaperSurfaceStyle
import com.heliactyl.bororwjabbilai.ui.paper.paperSurface
import com.heliactyl.bororwjabbilai.ui.theme.paper.PaperTheme
import androidx.core.content.edit
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.heliactyl.bororwjabbilai.BuildConfig
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.google.gson.Gson
import com.heliactyl.bororwjabbilai.FavoritesRepository
import com.heliactyl.bororwjabbilai.GitHubRelease
import com.heliactyl.bororwjabbilai.RecentsRepository
import com.heliactyl.bororwjabbilai.Song
import com.heliactyl.bororwjabbilai.SongRepository
import com.heliactyl.bororwjabbilai.ui.Occasion
import com.heliactyl.bororwjabbilai.ui.components.SocialRow
import com.heliactyl.bororwjabbilai.ui.components.SongItem
import com.heliactyl.bororwjabbilai.ui.components.bouncyClick
import com.heliactyl.bororwjabbilai.ui.components.glassOverlay
import com.heliactyl.bororwjabbilai.ui.components.liquidGlass
import com.heliactyl.bororwjabbilai.ui.glass.GlassEffectConfig
import com.heliactyl.bororwjabbilai.ui.glass.LocalAppBackdrop
import com.heliactyl.bororwjabbilai.ui.glass.LocalGlassEffectConfig
import com.heliactyl.bororwjabbilai.ui.glass.backdrop.backdrops.layerBackdrop
import com.heliactyl.bororwjabbilai.ui.glass.backdrop.backdrops.rememberLayerBackdrop
import com.heliactyl.bororwjabbilai.ui.occasionFilters
import com.heliactyl.bororwjabbilai.ui.screens.SongDetailScreen
import com.heliactyl.bororwjabbilai.ui.screens.SongListScreen
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SongApp(
    songRepository: SongRepository,
    recentsRepository: RecentsRepository,
    favoritesRepository: FavoritesRepository,
    isDarkTheme: Boolean,
    themeMode: Int = 0,
    onThemeCycle: (Offset) -> Unit,
    onThemeSelect: (Int, Offset) -> Unit = { _, offset -> onThemeCycle(offset) },
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val customSongRepository = remember { com.heliactyl.bororwjabbilai.CustomSongRepository(context) }
    val updateManager = remember { com.heliactyl.bororwjabbilai.UpdateManager(context) }

    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var customSongs by remember { mutableStateOf(customSongRepository.getCustomSongs()) }

    val allSongs = remember(songs, customSongs) { songs + customSongs }
    val songMap = remember(allSongs) { allSongs.associateBy { it.id } }

    var recentIds by remember { mutableStateOf(recentsRepository.getRecentIds()) }
    var favoriteIds by remember { mutableStateOf(favoritesRepository.getFavoriteIds()) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var editingSong by remember { mutableStateOf<Song?>(null) }
    var showEditor by remember { mutableStateOf(false) }

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

    var isNavCollapsed by remember { mutableStateOf(false) }

    val activeListState = when (pagerState.currentPage) {
        0 -> favoritesListState
        1 -> homeListState
        else -> recentsListState
    }

    LaunchedEffect(activeListState) {
        var prevIndex = activeListState.firstVisibleItemIndex
        var prevOffset = activeListState.firstVisibleItemScrollOffset

        snapshotFlow { activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index > prevIndex || (index == prevIndex && offset > prevOffset + 25)) {
                    if ((index > 0 || offset > 100) && !isNavCollapsed) {
                        isNavCollapsed = true
                    }
                } else if (index < prevIndex || (index == prevIndex && offset < prevOffset - 25) || (index == 0 && offset < 50)) {
                    if (isNavCollapsed) {
                        isNavCollapsed = false
                    }
                }
                prevIndex = index
                prevOffset = offset
            }
    }

    val animatedNavWidth by animateDpAsState(
        targetValue = if (isNavCollapsed) 64.dp else 320.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navWidth"
    )

    val animatedNavHeight by animateDpAsState(
        targetValue = if (isNavCollapsed) 64.dp else 72.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navHeight"
    )

    var pullUpOffset by remember { mutableFloatStateOf(0f) }

    val animatedPullUpOffset by animateFloatAsState(
        targetValue = pullUpOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "pullUpOffset"
    )

    val rubberBandStretchX = 1f + (animatedPullUpOffset / 220f)
    val rubberBandStretchY = (1f - (animatedPullUpOffset / 400f)).coerceAtLeast(0.8f)

    var isNavPressed by remember { mutableStateOf(false) }

    val bouncyNavScale by animateFloatAsState(
        targetValue = if (isNavPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bouncyNavScale"
    )

    var query by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }

    // Debounce keystrokes: the full-library filter + lyric snippet scan
    // runs once per typing pause instead of once per character.
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) {
        if (query.isBlank()) {
            debouncedQuery = query
        } else {
            delay(150)
            debouncedQuery = query
        }
    }

    // Predictive back handlers for active overlay states
    BackHandler(enabled = active) { active = false }
    BackHandler(enabled = showFilterSheet) { showFilterSheet = false }
    BackHandler(enabled = showInfoDialog) { showInfoDialog = false }
    BackHandler(enabled = updateRelease != null) { updateRelease = null }

    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    var hasSeenSwipeTutorial by remember { mutableStateOf(prefs.getBoolean("has_seen_swipe_tutorial", false)) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            updateManager.clearCache()
            songs = songRepository.getSongs()
        }
        
        // Only check for updates once a day
        val lastCheckDate = prefs.getString("last_update_check_date", "")
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        
        if (lastCheckDate != currentDate) {
            try {
                updateRelease = updateManager.checkForUpdate()
                // If we reach here, the check was successful (even if updateRelease is null)
                prefs.edit { putString("last_update_check_date", currentDate) }
            } catch (_: Exception) {
                // Ignore error (no internet etc), don't update lastCheckDate to retry on next startup
            }
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

    // ── Filter bottom sheet ───────────────────────────────────────────────────
    // ── Echo-style true glass backdrop ────────────────────────────────────
    // Records scrolling list content; fixed overlays (bottom bar, search bar,
    // filter sheet, dialogs) sample it via trueLiquidGlass (lens + blur +
    // vibrancy). Provided ABOVE the sheet/dialogs so their glassOverlay still
    // resolves LocalAppBackdrop inside those windows' subcompositions.
    // Falls back to translucent material on pre-S devices (see fallbackGlass).
    val appBackdrop = rememberLayerBackdrop { drawContent() }
    val glassConfig = remember { GlassEffectConfig() }

    CompositionLocalProvider(
        LocalAppBackdrop provides appBackdrop,
        LocalGlassEffectConfig provides glassConfig,
    ) {

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
                    .glassOverlay(cornerRadius = 32)
                    .padding(bottom = 32.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // ── Tab selector ──────────────────────────────────────────────
                // Fixed inside the sheet's dialog window, OUTSIDE the recorded
                // pager — safe to sample the backdrop for true glass.
                TabRow(
                    selectedTabIndex = filterPagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .height(52.dp)
                        .glassOverlay(
                            cornerRadius = 26,
                            color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f)
                                Color.White.copy(alpha = 0.04f)
                            else
                                Color.Black.copy(alpha = 0.02f)
                        )
                        // Specular rim drawn ABOVE the sampled backdrop (edge
                        // effects are skipped on HyperOS, so keep a border).
                        .border(
                            width = 1.dp,
                            brush = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f)
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.08f),
                                    )
                                )
                            else
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 1f),
                                        Color.Black.copy(alpha = 0.25f),
                                    )
                                ),
                            shape = RoundedCornerShape(26.dp),
                        ),
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                            val continuousPage = filterPagerState.currentPage + filterPagerState.currentPageOffsetFraction
                            val index = continuousPage.toInt().coerceIn(0, tabPositions.size - 2)
                            val fraction = continuousPage - index

                            val currentTab = tabPositions[index]
                            val nextTab = tabPositions[index + 1]

                            val lerpLeft = currentTab.left + (nextTab.left - currentTab.left) * fraction
                            val lerpWidth = currentTab.width + (nextTab.width - currentTab.width) * fraction

                            // Organic sine-stretch while sliding
                            val stretchProgress = kotlin.math.sin(fraction * kotlin.math.PI.toFloat())
                            val baseWidth = 85.dp
                            val maxStretch = 40.dp
                            val dynamicWidth = baseWidth + (maxStretch * stretchProgress)

                            val tabCenter = lerpLeft + (lerpWidth / 2)
                            val bubbleOffset = tabCenter - (dynamicWidth / 2)

                            // Clip wrapper prevents pill bleeding outside rounded row bounds
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))) {
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .width(dynamicWidth)
                                        .offset { IntOffset(x = bubbleOffset.roundToPx(), y = 0) }
                                        .padding(vertical = 8.dp)
                                        .zIndex(1f)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            if (isDark) Color.White.copy(alpha = 0.12f)
                                            else Color.White.copy(alpha = 0.90f)
                                        )
                                        .liquidGlass(
                                            cornerRadius = 24, 
                                            color = Color.Transparent, 
                                            blurRadius = 24f,
                                            borderWidth = if (isDark) 1f else 1.8f
                                        )
                                ) {
                                    // Top specular highlight for elegance
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.40f)
                                            .align(Alignment.TopCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = if (isDark) 0.25f else 0.40f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Tab(
                        selected = filterPagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { filterPagerState.animateScrollToPage(0) } },
                        modifier = Modifier.zIndex(2f),
                        text = {
                            Text(
                                "Letter",
                                fontWeight = if (filterPagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Tab(
                        selected = filterPagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { filterPagerState.animateScrollToPage(1) } },
                        modifier = Modifier.zIndex(2f),
                        text = {
                            Text(
                                "Category",
                                fontWeight = if (filterPagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // ── Fixed-height pager — no animateContentSize, no layout shifts ──
                HorizontalPager(
                    state = filterPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                ) { page ->
                    when (page) {
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
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
                                        }) { Text("Clear") }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                val chars = remember { ('A'..'Z').map { it.toString() } + "@" }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    chars.forEach { char ->
                                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                                        val selected = filterChar == char
                                        Box(
                                            modifier = Modifier
                                                .liquidGlass(
                                                    color = if (selected)
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    else
                                                        if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                                                    cornerRadius = 12,
                                                    borderWidth = if (selected) 1.6f else null
                                                )
                                                .bouncyClick {
                                                    filterChar = if (selected) null else char
                                                    filterOccasion = null
                                                    showFilterSheet = false
                                                }
                                        ) {
                                            // Specular top highlight — glass pill treatment.
                                            Box(
                                                Modifier
                                                    .matchParentSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            0f to Color.White.copy(alpha = if (isDark) 0.16f else 0.34f),
                                                            0.45f to Color.Transparent,
                                                        )
                                                    )
                                            )
                                            Box(
                                                Modifier
                                                    .align(Alignment.Center)
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = char,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (selected)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        }
                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
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
                                        }) { Text("Clear") }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                androidx.compose.foundation.lazy.LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(occasionFilters.size) { index ->
                                        val occasion = occasionFilters[index]
                                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                                        val selected = filterOccasion == occasion
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .liquidGlass(
                                                    color = if (selected)
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    else
                                                        if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                                                    cornerRadius = 16,
                                                    borderWidth = if (selected) 1.6f else null
                                                )
                                                .bouncyClick {
                                                    filterOccasion = if (selected) null else occasion
                                                    filterChar = null
                                                    showFilterSheet = false
                                                }
                                        ) {
                                            // Specular top highlight — glass pill treatment.
                                            Box(
                                                Modifier
                                                    .matchParentSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            0f to Color.White.copy(alpha = if (isDark) 0.14f else 0.30f),
                                                            0.45f to Color.Transparent,
                                                        )
                                                    )
                                            )
                                            Box(Modifier.padding(16.dp)) {
                                                Text(
                                                    text = occasion.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (selected)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Developer info dialog ─────────────────────────────────────────────────
    if (showInfoDialog) {
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .components { add(SvgDecoder.Factory()) }
                .build()
        }
        Dialog(
            onDismissRequest = { showInfoDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassOverlay(cornerRadius = 32)
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
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/heliactyl")))
                            }
                        )
                        SocialRow(
                            text = "Telegram",
                            handle = "@heliactyl",
                            iconModel = "file:///android_asset/telegram.svg",
                            imageLoader = imageLoader,
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/heliactyl")))
                            }
                        )
                        SocialRow(
                            text = "GitHub",
                            handle = "Badmaneers",
                            iconModel = "file:///android_asset/github.svg",
                            imageLoader = imageLoader,
                            tint = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White else Color.Black,
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Badmaneers")))
                            }
                        )
                        SocialRow(
                            text = "Email",
                            handle = "dukebrahma6@gmail.com",
                            iconVector = Icons.Default.Email,
                            imageLoader = imageLoader,
                            tint = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White else Color.Black,
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:dukebrahma6@gmail.com") }
                                )
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

    // ── Update available dialog ───────────────────────────────────────────────
    val release = updateRelease
    if (release != null) {
        var isDownloading by remember { mutableStateOf(false) }
        var downloadProgress by remember { mutableFloatStateOf(0f) }
        var downloaded by remember { mutableStateOf(updateManager.hasDownloadedApk()) }
        var needsPermission by remember { mutableStateOf(false) }

        // If Settings was opened for the install permission, fire the
        // package installer automatically when the user comes back granted.
        val dialogView = LocalView.current
        DisposableEffect(dialogView, needsPermission) {
            val treeObserver = dialogView.viewTreeObserver.takeIf { it.isAlive }
            val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                if (hasFocus && needsPermission &&
                    updateManager.canRequestInstall() && updateManager.hasDownloadedApk()
                ) {
                    if (updateManager.installApk()) {
                        needsPermission = false
                        updateRelease = null
                    }
                }
            }
            treeObserver?.addOnWindowFocusChangeListener(listener)
            onDispose {
                runCatching {
                    if (treeObserver?.isAlive == true) {
                        treeObserver.removeOnWindowFocusChangeListener(listener)
                    }
                }
            }
        }

        Dialog(
            onDismissRequest = {
                if (!isDownloading) updateRelease = null
            },
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
                        .glassOverlay(cornerRadius = 32)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                "Update Available!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Version ${release.tagName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (release.body.isNotBlank()) {
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .liquidGlass(
                                    color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                                    cornerRadius = 16
                                )
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = release.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 20.sp
                            )
                        }
                    }

                    if (isDownloading) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "Downloading... ${(downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        if (needsPermission) {
                            Text(
                                text = "Install permission is off — enable 'Install unknown apps' in Settings, then come back; the installer opens automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { updateRelease = null }
                            ) {
                                Text("Later")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val apkAsset = release.assets.find { it.name.endsWith(".apk") } ?: release.assets.firstOrNull()
                                    if (apkAsset != null) {
                                        coroutineScope.launch {
                                            isDownloading = true
                                            downloadProgress = 0f
                                            try {
                                                if (!downloaded) {
                                                    updateManager.downloadApk(apkAsset.downloadUrl).collect { progress ->
                                                        downloadProgress = progress
                                                    }
                                                    downloaded = true
                                                }
                                                if (updateManager.installApk()) {
                                                    needsPermission = false
                                                    updateRelease = null
                                                } else {
                                                    needsPermission = true
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Download failed: ${e.localizedMessage}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } finally {
                                                isDownloading = false
                                            }
                                        }
                                    } else {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl)))
                                        updateRelease = null
                                    }
                                }
                            ) {
                                Text(if (downloaded) "Open Installer" else "Update Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Interactive detail state ──────────────────────────────────────────────
    val detailSong = remember { mutableStateOf<Song?>(null) }
    LaunchedEffect(selectedSong) {
        if (selectedSong != null) detailSong.value = selectedSong
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val baseBlur by animateDpAsState(
            targetValue = if (showFilterSheet || showInfoDialog || updateRelease != null || active) 10.dp else 0.dp,
            animationSpec = tween(durationMillis = 300),
            label = "baseBlur"
        )
        val blurFraction by remember(sheetState, showInfoDialog, updateRelease, active) {
            derivedStateOf {
                if (showInfoDialog || updateRelease != null || active) return@derivedStateOf 1f
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

        // 1. MAIN APP CONTENT (BLURRED)
        Box(modifier = Modifier.fillMaxSize().blur(blurRadius)) {
            Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { _ ->
                val topBarHeight = 140.dp
                val bottomBarHeight = 110.dp

                Box(Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(appBackdrop)
                    ) { page ->
                        val screenPadding = PaddingValues(top = topBarHeight, bottom = bottomBarHeight)
                        when (page) {
                            0 -> {
                                val favoriteSongs = remember(songMap, favoriteIds) {
                                    favoriteIds.mapNotNull { songMap[it] }
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
                                val recentSongs = remember(songMap, recentIds) {
                                    recentIds.mapNotNull { songMap[it] }
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

                    // Floating Liquid Bottom Bar
                    if (!showEditor && selectedSong == null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .offset { IntOffset(0, -animatedPullUpOffset.roundToInt()) }
                                .graphicsLayer {
                                    scaleX = rubberBandStretchX * bouncyNavScale
                                    scaleY = rubberBandStretchY * bouncyNavScale
                                }
                                .navigationBarsPadding()
                                .size(animatedNavWidth, animatedNavHeight)
                                .glassOverlay(cornerRadius = 32)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isNavPressed = true
                                            tryAwaitRelease()
                                            isNavPressed = false
                                        },
                                        onTap = {
                                            if (isNavCollapsed) {
                                                isNavCollapsed = false
                                            }
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            isNavPressed = true
                                            pullUpOffset = 0f
                                        },
                                        onDragEnd = {
                                            isNavPressed = false
                                            pullUpOffset = 0f
                                        },
                                        onDragCancel = {
                                            isNavPressed = false
                                            pullUpOffset = 0f
                                        }
                                    ) { change, dragAmount ->
                                        if (pagerState.currentPage == 1) {
                                            if (dragAmount < 0) {
                                                pullUpOffset = (pullUpOffset - dragAmount * 0.6f).coerceIn(0f, 60f)
                                                if (pullUpOffset > 30f && !showFilterSheet) {
                                                    showFilterSheet = true
                                                    pullUpOffset = 0f
                                                    if (!hasSeenSwipeTutorial) {
                                                        hasSeenSwipeTutorial = true
                                                        prefs.edit().putBoolean("has_seen_swipe_tutorial", true).apply()
                                                    }
                                                }
                                                change.consume()
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = isNavCollapsed,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.75f))
                                        .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.75f))
                                },
                                label = "navMorph"
                            ) { collapsed ->
                                if (collapsed) {
                                    // Single Liquid Bouncy Icon (Collapsed)
                                    val currentIcon = when (pagerState.currentPage) {
                                        0 -> Icons.Default.Favorite
                                        1 -> Icons.Default.Home
                                        else -> Icons.Default.History
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = currentIcon,
                                            contentDescription = "Expand Navigation Bar",
                                            tint = PaperTheme.colors.accentInk,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                } else {
                                // Expanded 3-Tab Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            detectVerticalDragGestures(
                                                onDragStart = { pullUpOffset = 0f },
                                                onDragEnd = { pullUpOffset = 0f },
                                                onDragCancel = { pullUpOffset = 0f }
                                            ) { change, dragAmount ->
                                                if (pagerState.currentPage == 1) {
                                                    if (dragAmount < 0) {
                                                        pullUpOffset = (pullUpOffset - dragAmount * 0.6f).coerceIn(0f, 60f)
                                                        if (pullUpOffset > 30f && !showFilterSheet) {
                                                            showFilterSheet = true
                                                            pullUpOffset = 0f
                                                            if (!hasSeenSwipeTutorial) {
                                                                hasSeenSwipeTutorial = true
                                                                prefs.edit().putBoolean("has_seen_swipe_tutorial", true).apply()
                                                            }
                                                        }
                                                        change.consume()
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    val continuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                    val baseWidth = 90.dp
                                    val bubbleHeight = 52.dp

                                    Box(Modifier.fillMaxSize()) {
                                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

                                        BoxWithConstraints(Modifier.fillMaxSize()) {
                                            val totalWidthPx = with(LocalDensity.current) { this@BoxWithConstraints.maxWidth.toPx() }
                                            val slotWidthPx = totalWidthPx / 3f

                                            val targetCenterPx = (continuousPage + 0.5f) * slotWidthPx

                                            val pageIndex = continuousPage.toInt()
                                            val fraction = continuousPage - pageIndex
                                            val stretchProgress = sin(fraction * PI.toFloat())
                                            val maxStretch = 30.dp
                                            val dynamicWidth = baseWidth + (maxStretch * stretchProgress)

                                            val bubbleWidthPx = with(LocalDensity.current) { dynamicWidth.toPx() }
                                            val bubbleOffsetPx = targetCenterPx - (bubbleWidthPx / 2f)

                                            Box(
                                                Modifier
                                                    .offset { IntOffset(bubbleOffsetPx.roundToInt(), 0) }
                                                    .align(Alignment.CenterStart)
                                                    .size(dynamicWidth, bubbleHeight)
                                                    .zIndex(0f)
                                                    .clip(RoundedCornerShape(22.dp))
                                                    .background(
                                                        if (isDark) Color.White.copy(alpha = 0.12f)
                                                        else Color.White.copy(alpha = 0.90f)
                                                    )
                                                    .liquidGlass(
                                                        cornerRadius = 22,
                                                        color = Color.Transparent,
                                                        blurRadius = 22f,
                                                        borderWidth = if (isDark) 1f else 1.8f
                                                    )
                                            )
                                        }
                                    }

                                    NavigationBar(
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp,
                                        modifier = Modifier.zIndex(1f)
                                    ) {
                                        val navItemColors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color.Transparent,
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )

                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    Icons.Default.Favorite,
                                                    contentDescription = "Saved",
                                                    modifier = Modifier.bouncyClick {
                                                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                                    }
                                                )
                                            },
                                            label = { Text("Saved") },
                                            selected = pagerState.currentPage == 0,
                                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                                            colors = navItemColors
                                        )
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    Icons.Default.Home,
                                                    contentDescription = "Home",
                                                    modifier = Modifier.bouncyClick {
                                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                                    }
                                                )
                                            },
                                            label = { Text("Home") },
                                            selected = pagerState.currentPage == 1,
                                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                                            colors = navItemColors
                                        )
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    Icons.Default.History,
                                                    contentDescription = "Recents",
                                                    modifier = Modifier.bouncyClick {
                                                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                                    }
                                                )
                                            },
                                            label = { Text("Recents") },
                                            selected = pagerState.currentPage == 2,
                                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                                            colors = navItemColors
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Swipe-up tutorial hint
                    if (!hasSeenSwipeTutorial && pagerState.currentPage == 1 && selectedSong == null) {
                        val infiniteTransition = rememberInfiniteTransition(label = "tutorial")
                        val yOffset by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = -100f,
                            animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                            label = "yOffset"
                        )
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 1f, targetValue = 0f,
                            animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
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
                                    Text("Swipe Up to Filter", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Icon(Icons.Default.TouchApp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }
            }
        }
    }

        // 1.2 SEARCH SCRIM — while search is open the blurred page must not
        // receive touches; tapping outside dismisses search instead.
        if (active) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { active = false }
                    )
            )
        }

        // 1.5 FLOATING SEARCH BAR — fixed overlay OUTSIDE layerBackdrop.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val fullWidth = maxWidth
            val collapsedSize = 56.dp
            val isSearchCollapsed = isNavCollapsed && !active

            val animatedSearchWidth by animateDpAsState(
                targetValue = if (isSearchCollapsed) collapsedSize else fullWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "searchWidth"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(animatedSearchWidth)
                    .then(
                        if (active) Modifier.wrapContentHeight()
                        else Modifier.height(56.dp)
                    )
                    .graphicsLayer {
                        clip = true
                        shape = RoundedCornerShape(28.dp)
                    }
                    .glassOverlay(cornerRadius = 28),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isSearchCollapsed,
                    transitionSpec = {
                        if (targetState) {
                            // Collapsing into top-right circle icon
                            (fadeIn(animationSpec = tween(180, delayMillis = 60)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                                .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.85f, animationSpec = tween(120)))
                        } else {
                            // Uncollapsing into normal resting search bar
                            (fadeIn(animationSpec = tween(180, delayMillis = 60)) + scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                                .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.8f, animationSpec = tween(120)))
                        }
                    },
                    label = "searchMorph"
                ) { collapsed ->
                    if (collapsed) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .bouncyClick {
                                    isNavCollapsed = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Expand Search Bar",
                                tint = PaperTheme.colors.accentInk,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else {
                        DockedSearchBar(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { active = false },
                            active = active,
                            onActiveChange = { active = it },
                            placeholder = { Text("Search song...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.bouncyClick { active = true }
                                )
                            },
                            colors = SearchBarDefaults.colors(
                                containerColor = Color.Transparent,
                            ),
                            trailingIcon = {
                                if (active || query.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            if (query.isNotEmpty()) query = "" else active = false
                                        },
                                        modifier = Modifier.bouncyClick()
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                } else {
                                    Row {
                                        if (BuildConfig.DEBUG) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val url = URL("https://api.github.com/repos/Badmaneers/bororwjabbilai/releases/latest")
                                                        var errorDetail = ""
                                                        val result = withContext(Dispatchers.IO) {
                                                            try {
                                                                val conn = url.openConnection() as HttpURLConnection
                                                                conn.setRequestProperty("User-Agent", "BoroRwjabBilai-App")
                                                                if (conn.responseCode == 200) {
                                                                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                                                                    Gson().fromJson(json, GitHubRelease::class.java)
                                                                } else {
                                                                    errorDetail = "HTTP ${conn.responseCode}"
                                                                    null
                                                                }
                                                            } catch (e: Exception) {
                                                                errorDetail = e.localizedMessage ?: "Unknown error"
                                                                null
                                                            }
                                                        }
                                                        if (result != null) {
                                                            updateRelease = result
                                                        } else {
                                                            Toast.makeText(context, "Update check failed: $errorDetail", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.bouncyClick()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BugReport,
                                                    contentDescription = "Test Update",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { showEditor = true },
                                            modifier = Modifier.bouncyClick()
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Custom Song", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        var themeButtonCenter by remember { mutableStateOf(Offset.Zero) }
                                        IconButton(
                                            modifier = Modifier
                                                .onGloballyPositioned {
                                                    val rootPos = it.positionInRoot()
                                                    val size = it.size
                                                    themeButtonCenter = Offset(rootPos.x + size.width / 2f, rootPos.y + size.height / 2f)
                                                }
                                                .bouncyClick(),
                                            onClick = { onThemeCycle(themeButtonCenter) }
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkTheme) Icons.Default.NightsStay else Icons.Default.WbSunny,
                                                contentDescription = "Toggle Theme",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { showInfoDialog = true },
                                            modifier = Modifier.bouncyClick()
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = "Developer Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val favoriteSet = remember(favoriteIds) { favoriteIds.toSet() }

                            val filteredSongsWithSnippets = remember(debouncedQuery, allSongs) {
                                if (debouncedQuery.isBlank()) allSongs.map { it to null }
                                else {
                                    val q = debouncedQuery.trim().lowercase()
                                    allSongs.mapNotNull { song ->
                                        val allLyrics = song.searchLyricsText
                                        val titleMatch = song.searchTitleLower.contains(q)
                                        val idMatch = song.searchIdString.contains(q)
                                        val lyricsMatch = allLyrics.contains(q, ignoreCase = true)

                                        if (titleMatch || idMatch || lyricsMatch) {
                                            val snippet = if (lyricsMatch) {
                                                val index = allLyrics.indexOf(q, ignoreCase = true)
                                                val start = (index - 30).coerceAtLeast(0)
                                                val end = (index + q.length + 40).coerceAtMost(allLyrics.length)
                                                val rawSnippet = allLyrics.substring(start, end).trim()
                                                
                                                val prefix = if (start > 0) "..." else ""
                                                val suffix = if (end < allLyrics.length) "..." else ""
                                                "$prefix$rawSnippet$suffix"
                                            } else if (song.lyrics.isNotEmpty() && song.lyrics[0].lines.isNotEmpty()) {
                                                song.lyrics[0].lines[0]
                                            } else null
                                            
                                            song to snippet
                                        } else null
                                    }
                                }
                            }
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredSongsWithSnippets, key = { (song, _) -> song.id }) { (song, snippet) ->
                                    SongItem(
                                        song = song,
                                        isFavorite = song.id in favoriteSet,
                                        snippet = snippet,
                                        onFavoriteClick = { toggleFavorite(song) },
                                        onClick = {
                                            keyboardController?.hide()
                                            selectedSong = song
                                            recentsRepository.addRecent(song.id)
                                            recentIds = recentsRepository.getRecentIds()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. SONG DETAIL OVERLAY
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
    }
}
}
