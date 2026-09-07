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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

fun shareLyrics(context: Context, song: Song, textToShare: String? = null) {
    val shareText = if (textToShare != null) {
        "\"$textToShare\"\n\n— From: ${song.title}"
    } else {
        if (song.isCustom) {
            // Sharing custom song code
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
                                                    // Bouncy A- button
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .liquidGlass(cornerRadius = 14)
                                                            .bouncyClick {
                                                                if (fontSize > 12f) {
                                                                    fontSize -= 2f
                                                                    prefs.edit { putFloat("font_size", fontSize) }
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
                                                    
                                                    // Bouncy A+ button
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .liquidGlass(cornerRadius = 14)
                                                            .bouncyClick {
                                                                if (fontSize < 40f) {
                                                                    fontSize += 2f
                                                                    prefs.edit { putFloat("font_size", fontSize) }
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("A+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                    .padding(paddingValues)
            ) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(24.dp)
                            .padding(bottom = 80.dp)
                    ) {
                        song.lyrics.forEach { section ->
                            LyricSectionItem(
                                section = section,
                                fontSize = fontSize
                            )
                            Spacer(modifier = Modifier.height(24.dp))
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
            }
        }
    }
}

@Composable
fun LyricSectionItem(
    section: LyricSection,
    fontSize: Float
) {
    val isChorus = section.type == "chorus"
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isChorus) {
            Text(
                text = "Chorus",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = (fontSize - 2).sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        section.lines.forEachIndexed { index, line ->
            val numberPrefix = if (!isChorus && index == 0 && section.number != null) "${section.number}. " else ""
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClick()
                    .padding(vertical = 3.dp)
            ) {
                Row {
                    if (numberPrefix.isNotEmpty()) {
                        Text(
                            text = numberPrefix,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize.sp,
                            fontStyle = if (isChorus) FontStyle.Italic else FontStyle.Normal,
                            lineHeight = (fontSize * 1.45f).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
