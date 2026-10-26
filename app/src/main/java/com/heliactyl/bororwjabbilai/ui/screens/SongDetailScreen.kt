package com.heliactyl.bororwjabbilai.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import com.heliactyl.bororwjabbilai.CustomSongRepository
import com.heliactyl.bororwjabbilai.LyricSection
import com.heliactyl.bororwjabbilai.Song
import com.heliactyl.bororwjabbilai.ui.components.bouncyClick
import com.heliactyl.bororwjabbilai.ui.components.liquidGlass
import com.heliactyl.bororwjabbilai.ui.paper.PaperSurfaceCanvas
import com.heliactyl.bororwjabbilai.ui.paper.PaperSurfaceStyle
import com.heliactyl.bororwjabbilai.ui.theme.PencilPeteFontFamily
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.math.sqrt

fun shareLyrics(context: Context, song: Song, textToShare: String? = null) {
    val shareText = if (textToShare != null) {
        "\"$textToShare\"\n\n— From: ${song.title}"
    } else {
        if (song.isCustom) {
            val repo = CustomSongRepository(context)
            val code = repo.exportSongAsCode(song)
            "Custom Song: ${song.title}\n\nImport this song into Boro Rwjab Bilai app using this code:\n\n$code"
        } else {
            val fullLyrics = song.lyrics.joinToString("\n\n") { section ->
                val numberPrefix = if ((section.type != "chorus") && (section.number != null)) "${section.number}. " else ""
                val linesText = section.lines.joinToString("\n")
                "$numberPrefix$linesText"
            }
            "Song Title: ${song.title}\n\n$fullLyrics\n\nShared from Boro Rwjab Bilai App"
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Song"))
}

@Composable
fun SongDetailScreen(song: Song, onBack: () -> Unit, onEdit: ((Song) -> Unit)? = null) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", false)) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("font_size", 18f)) }
    var paperStyleName by remember { mutableStateOf(prefs.getString("paper_style", "ruled") ?: "ruled") }

    val paperSurfaceStyle = when (paperStyleName) {
        "flat" -> PaperSurfaceStyle.FLAT
        else -> PaperSurfaceStyle.RULED
    }

    val rowHeight = (fontSize * 1.55f).dp

    var showPinchHud by remember { mutableStateOf(false) }
    var hudTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(hudTick) {
        if (hudTick > 0) {
            showPinchHud = true
            delay(800)
            showPinchHud = false
        }
    }

    LaunchedEffect(fontSize) {
        delay(300)
        prefs.edit { putFloat("font_size", fontSize) }
    }

    var notificationMessage by remember { mutableStateOf("") }
    var showNotification by remember { mutableStateOf(false) }

    var predictiveProgress by remember(song) { mutableFloatStateOf(0f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var isDraggingGesture by remember { mutableStateOf(false) }

    LaunchedEffect(song) {
        predictiveProgress = 0f
    }

    PredictiveBackHandler { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                predictiveProgress = backEvent.progress
                swipeEdge = backEvent.swipeEdge
            }
            predictiveProgress = 0f
            onBack()
        } catch (_: CancellationException) {
            predictiveProgress = 0f
        } finally {
            predictiveProgress = 0f
        }
    }

    LaunchedEffect(showNotification) {
        if (showNotification) {
            delay(2000)
            showNotification = false
        }
    }

    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = predictiveProgress,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy,
        ),
        label = "unsnapProgress"
    )

    val densityPx = density.density
    val safeProgress = animatedProgress.coerceIn(0f, 1f)
    val unsnapScale = (1f - (safeProgress * 0.12f)).coerceIn(0.1f, 1f)
    val unsnapCornerRadius = (safeProgress * 28).dp.coerceAtLeast(0.dp)
    val unsnapTranslationX = if (swipeEdge == BackEventCompat.EDGE_RIGHT) {
        -safeProgress * 60f * densityPx
    } else {
        safeProgress * 60f * densityPx
    }
    val unsnapTranslationY = safeProgress * 20f * densityPx
    val unsnapAlpha = (1f - (safeProgress * 0.15f)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = unsnapScale
                scaleY = unsnapScale
                translationX = unsnapTranslationX
                translationY = unsnapTranslationY
                alpha = unsnapAlpha
                shadowElevation = safeProgress * 16f * densityPx
                shape = RoundedCornerShape(unsnapCornerRadius)
                clip = true
            }
    ) {
        Scaffold(
            modifier = Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDraggingGesture = true
                        swipeEdge = BackEventCompat.EDGE_LEFT
                    },
                    onDragEnd = {
                        val shouldBack = predictiveProgress > 0.35f
                        isDraggingGesture = false
                        predictiveProgress = 0f
                        if (shouldBack) {
                            onBack()
                        }
                    },
                    onDragCancel = {
                        isDraggingGesture = false
                        predictiveProgress = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (isDraggingGesture) {
                            val newProgress = (predictiveProgress + (dragAmount / 600f)).coerceIn(0f, 1f)
                            predictiveProgress = newProgress
                        }
                    }
                )
            },
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .liquidGlass(cornerRadius = 20)
                        .bouncyClick { shareLyrics(context, song, null) }
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Song",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 0)
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .align(Alignment.Top)
                                .bouncyClick(onClick = onBack)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = "${song.id}. ${song.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .padding(vertical = 12.dp)
                        )

                        Row(modifier = Modifier.align(Alignment.Top)) {
                            if (song.isCustom && onEdit != null) {
                                IconButton(onClick = { onEdit(song) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Song",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            var showTextSettings by remember { mutableStateOf(false) }
                            
                            Box {
                                IconButton(onClick = { showTextSettings = true }) {
                                    Icon(
                                        imageVector = Icons.Default.TextFields,
                                        contentDescription = "Text Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                if (showTextSettings) {
                                    Popup(
                                        onDismissRequest = { showTextSettings = false },
                                        offset = IntOffset(0, 140),
                                        alignment = Alignment.TopEnd,
                                        properties = PopupProperties(focusable = true)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(220.dp)
                                                .liquidGlass(cornerRadius = 24)
                                                .padding(16.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    "Font Size",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .liquidGlass(cornerRadius = 14)
                                                            .bouncyClick {
                                                                if (fontSize > 12f) {
                                                                    fontSize -= 2f
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("A-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    }
                                                    
                                                    Text(
                                                        text = fontSize.toInt().toString(),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .liquidGlass(cornerRadius = 14)
                                                            .bouncyClick {
                                                                if (fontSize < 40f) {
                                                                    fontSize += 2f
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("A+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    "Paper Style",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )

                                                val paperStyles = listOf(
                                                    "ruled" to "Ruled Notebook",
                                                    "flat" to "Flat Paper"
                                                )

                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    paperStyles.forEach { (styleKey, styleLabel) ->
                                                        val isSelected = paperStyleName == styleKey
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .liquidGlass(
                                                                    color = if (isSelected)
                                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                                    else
                                                                        Color.White.copy(alpha = 0.05f),
                                                                    cornerRadius = 12
                                                                )
                                                                .bouncyClick {
                                                                    paperStyleName = styleKey
                                                                    prefs.edit { putString("paper_style", styleKey) }
                                                                }
                                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                        ) {
                                                            Text(
                                                                text = styleLabel,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            IconButton(onClick = { 
                                keepScreenOn = !keepScreenOn
                                prefs.edit { putBoolean("keep_screen_on", keepScreenOn) }
                                notificationMessage = if (keepScreenOn) "Screen awake enabled" else "Screen awake disabled"
                                showNotification = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Keep Screen Awake",
                                    tint = if (keepScreenOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            val scrollState = rememberScrollState()
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            var prevSpan = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val pointers = event.changes.filter { it.pressed }
                                if (pointers.size < 2) {
                                    prevSpan = 0f
                                    continue
                                }
                                val p1 = pointers[0].position
                                val p2 = pointers[1].position
                                val dx = p1.x - p2.x
                                val dy = p1.y - p2.y
                                val span = sqrt(dx * dx + dy * dy)

                                val prev = prevSpan
                                prevSpan = span
                                if (prev <= 0f) continue

                                val zoom = span / prev
                                if (abs(zoom - 1f) < 0.005f) continue

                                val newSize = (fontSize * zoom).coerceIn(12f, 40f)
                                if (newSize != fontSize) {
                                    fontSize = newSize
                                    hudTick++
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                    .padding(paddingValues)
            ) {
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        // PaperSurfaceCanvas is inside the scroll Box with matchParentSize()
                        // so rule lines grow to cover the full lyrics height AND scroll in 1:1 lockstep!
                        PaperSurfaceCanvas(
                            style = paperSurfaceStyle,
                            lineSpacing = rowHeight,
                            topPadding = 20.dp,
                            showMargin = paperSurfaceStyle == PaperSurfaceStyle.RULED,
                            modifier = Modifier.matchParentSize()
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 20.dp + rowHeight, // Starts on 2nd notebook line
                                    start = 48.dp,            // Positioned inside (to the right of) red margin line
                                    end = 16.dp,
                                    bottom = 80.dp
                                )
                        ) {
                            song.lyrics.forEach { section ->
                                LyricSectionItem(
                                    section = section,
                                    fontSize = fontSize,
                                    rowHeight = rowHeight
                                )
                                Spacer(modifier = Modifier.height(rowHeight))
                            }
                        }
                    }
                }

                // Custom Animated Toast Notification
                AnimatedVisibility(
                    visible = showNotification,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .zIndex(10f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        contentColor = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = if (keepScreenOn) Color.Yellow else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = notificationMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Font size HUD shown while pinch-zooming.
                AnimatedVisibility(
                    visible = showPinchHud,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(10f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        contentColor = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = fontSize.toInt().toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricSectionItem(
    section: LyricSection,
    fontSize: Float,
    rowHeight: Dp
) {
    val isChorus = section.type == "chorus"
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isChorus) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Chorus",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = (fontSize - 2).sp,
                        fontFamily = PencilPeteFontFamily
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        section.lines.forEachIndexed { index, line ->
            val numberPrefix = if (!isChorus && index == 0 && section.number != null) "${section.number}. " else ""
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .bouncyClick(),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (numberPrefix.isNotEmpty()) {
                        Text(
                            text = numberPrefix,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = fontSize.sp,
                                fontFamily = PencilPeteFontFamily
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize.sp,
                            fontFamily = PencilPeteFontFamily,
                            fontStyle = if (isChorus) FontStyle.Italic else FontStyle.Normal,
                            lineHeight = (fontSize * 1.45f).sp
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
