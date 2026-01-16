package com.heliactyl.bororwjabbilai.ui.screens

import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.heliactyl.bororwjabbilai.Song

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

    var isBackTriggered by remember { mutableStateOf(false) }
    
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
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.Top)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .padding(vertical = 12.dp)
                    )

                    Row(modifier = Modifier.align(Alignment.Top)) {
                        IconButton(onClick = { 
                            if (fontSize > 12f) {
                                fontSize -= 2f
                                prefs.edit().putFloat("font_size", fontSize).apply()
                            }
                        }) {
                            Text("A-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { 
                            if (fontSize < 40f) {
                                fontSize += 2f
                                prefs.edit().putFloat("font_size", fontSize).apply()
                            }
                        }) {
                            Text("A+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
