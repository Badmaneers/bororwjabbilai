package com.heliactyl.bororwjabbilai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.heliactyl.bororwjabbilai.ui.SongApp
import com.heliactyl.bororwjabbilai.ui.theme.BoroRwjabBilaiTheme
import kotlinx.coroutines.launch
import kotlin.math.hypot


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }

        display?.let { d ->
            val modes = d.supportedModes
            val currentMode = d.mode
            // Select mode with same resolution but highest refresh rate
            val highestRefreshRateMode = modes
                .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                .maxByOrNull { it.refreshRate }
                
            highestRefreshRateMode?.let { mode ->
                val layoutParams = window.attributes
                layoutParams.preferredDisplayModeId = mode.modeId
                window.attributes = layoutParams
            }
        }

        setContent {
            val systemDark = isSystemInDarkTheme()
            val context = LocalContext.current
            val view = LocalView.current
            val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
            val scope = rememberCoroutineScope()
            
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

            val screenshotState = remember { mutableStateOf<Bitmap?>(null) }
            val transitionOffset = remember { mutableStateOf(Offset.Zero) }
            val transitionRadius = remember { Animatable(0f) }

            Box(modifier = Modifier.fillMaxSize()) {
                if (screenshotState.value != null) {
                    Image(
                        bitmap = screenshotState.value!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                BoroRwjabBilaiTheme(darkTheme = useDarkTheme) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                if (screenshotState.value != null) {
                                    clipPath(
                                        path = Path().apply {
                                            addOval(
                                                Rect(
                                                    center = transitionOffset.value,
                                                    radius = transitionRadius.value
                                                )
                                            )
                                        }
                                    ) {
                                        this@drawWithContent.drawContent()
                                    }
                                } else {
                                    this@drawWithContent.drawContent()
                                }
                            },
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SongApp(
                            songRepository = SongRepository(context),
                            recentsRepository = RecentsRepository(context),
                            favoritesRepository = FavoritesRepository(context),
                            isDarkTheme = useDarkTheme,
                            onThemeCycle = { offset ->
                                val bitmap = Bitmap.createBitmap(
                                    view.width,
                                    view.height,
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = Canvas(bitmap)
                                view.draw(canvas)
                                
                                screenshotState.value = bitmap
                                transitionOffset.value = offset
                                
                                scope.launch {
                                    transitionRadius.snapTo(0f)
                                    
                                    val newMode = when (themeMode) {
                                        0 -> if (systemDark) 1 else 2
                                        1 -> 2
                                        else -> 1
                                    }
                                    themeMode = newMode
                                    prefs.edit().putInt("theme_mode", newMode).apply()

                                    val maxRadius = hypot(view.width.toFloat(), view.height.toFloat())
                                    transitionRadius.animateTo(maxRadius, animationSpec = tween(700))
                                    screenshotState.value = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
