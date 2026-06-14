package com.heliactyl.bororwjabbilai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.selection.SelectionContainer
import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.heliactyl.bororwjabbilai.LyricSection
import com.heliactyl.bororwjabbilai.Song
import com.heliactyl.bororwjabbilai.ui.components.liquidGlass
import com.heliactyl.bororwjabbilai.ui.components.bouncyClick
import kotlinx.coroutines.delay

fun shareLyrics(context: android.content.Context, song: Song, textToShare: String? = null) {
    val shareText = if (textToShare != null) {
        "\"$textToShare\"\n\n— From: ${song.title}"
    } else {
        if (song.isCustom) {
            // Sharing custom song code
            val repo = com.heliactyl.bororwjabbilai.CustomSongRepository(context)
            val code = repo.exportSongAsCode(song)
            "Custom Song: ${song.title}\n\nImport this song into Boro Rwjab Bilai app using this code:\n\n$code"
        } else {
            val fullLyrics = song.lyrics.joinToString("\n\n") { section ->
                val numberPrefix = if (section.type != "chorus" && section.number != null) "${section.number}. " else ""
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(song: Song, onBack: () -> Unit, onEdit: ((Song) -> Unit)? = null) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    
    var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", false)) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("font_size", 18f)) }

    var notificationMessage by remember { mutableStateOf("") }
    var showNotification by remember { mutableStateOf(false) }

    LaunchedEffect(showNotification) {
        if (showNotification) {
            delay(2000)
            showNotification = false
        }
    }

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

    var isBackTriggered by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    if (!isBackTriggered && dragAmount > 20) {
                        isBackTriggered = true
                        onBack()
                    }
                }
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
                                imageVector = Icons.Default.ArrowBack,
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
                                        offset = IntOffset(0, 140), // Position nicely below the icon
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
                                                                    prefs.edit().putFloat("font_size", fontSize).apply()
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("A-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    }
                                                    
                                                    Text(
                                                        text = "${fontSize.toInt()}",
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
                                                                    prefs.edit().putFloat("font_size", fontSize).apply()
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
                                prefs.edit().putBoolean("keep_screen_on", keepScreenOn).apply()
                                notificationMessage = if (keepScreenOn) "Screen awake enabled" else "Screen awake disabled"
                                showNotification = true
                            }) {
                                Icon(
                                    imageVector = if (keepScreenOn) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Keep Screen On",
                                    tint = if (keepScreenOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SelectionContainer {
                    Column {
                        song.lyrics.forEach { section ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .padding(end = 8.dp),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    if (section.type != "chorus" && section.number != null) {
                                        Text(
                                            text = "${section.number}.",
                                            fontSize = fontSize.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    section.lines.forEach { line ->
                                        Text(
                                            text = line,
                                            fontSize = fontSize.sp,
                                            lineHeight = (fontSize * 1.5).sp,
                                            fontStyle = if (section.type == "chorus") FontStyle.Italic else FontStyle.Normal,
                                            fontWeight = if (section.type == "chorus") FontWeight.SemiBold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // Spacer at bottom to avoid navigation bar overlap if not handled by padding
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Custom Glassy Notification
        AnimatedVisibility(
            visible = showNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .zIndex(100f)
        ) {
            Box(
                modifier = Modifier
                    .liquidGlass(cornerRadius = 24)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (keepScreenOn) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = notificationMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
