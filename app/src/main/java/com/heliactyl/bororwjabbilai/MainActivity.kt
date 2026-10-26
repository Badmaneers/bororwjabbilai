package com.heliactyl.bororwjabbilai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import com.heliactyl.bororwjabbilai.ui.SongApp
import com.heliactyl.bororwjabbilai.ui.theme.paper.PaperTheme
import com.heliactyl.bororwjabbilai.ui.theme.paper.PaperThemeMode
import kotlinx.coroutines.launch
import kotlin.math.hypot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
                .asSequence()
                .filter { (it.physicalWidth == currentMode.physicalWidth) && (it.physicalHeight == currentMode.physicalHeight) }
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
            val prefs = remember { context.getSharedPreferences("settings", MODE_PRIVATE) }
            val scope = rememberCoroutineScope()
            
            // 0: System, 1: Light, 2: Dark (Slate), 3: AMOLED
            var themeMode by remember(systemDark) { 
                val lastSystemDark = prefs.getBoolean("last_system_dark", systemDark)
                val initialMode = if (lastSystemDark != systemDark) {
                    val currentPref = prefs.getInt("theme_mode", 0) // Default to System Paper
                    prefs.edit { putBoolean("last_system_dark", systemDark) }
                    currentPref
                } else {
                    if (!prefs.contains("last_system_dark")) {
                        prefs.edit { putBoolean("last_system_dark", systemDark) }
                    }
                    prefs.getInt("theme_mode", 0) // Default to System Paper
                }
                mutableIntStateOf(initialMode)
            }

            val paperMode = when (themeMode) {
                1 -> PaperThemeMode.LIGHT
                2 -> PaperThemeMode.DARK
                3 -> PaperThemeMode.AMOLED
                else -> PaperThemeMode.SYSTEM
            }

            val useDarkTheme = when (paperMode) {
                PaperThemeMode.SYSTEM -> systemDark
                PaperThemeMode.LIGHT -> false
                PaperThemeMode.DARK, PaperThemeMode.AMOLED -> true
            }

            val screenshotState = remember { mutableStateOf<Bitmap?>(null) }
            val transitionOffset = remember { mutableStateOf(Offset.Zero) }
            val transitionRadius = remember { Animatable(0f) }

            val backgroundBrush = when (paperMode) {
                PaperThemeMode.LIGHT -> Brush.linearGradient(
                    colors = listOf(Color(0xFFF6F1E7), Color(0xFFEFE8DA)),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f),
                )
                PaperThemeMode.DARK -> Brush.linearGradient(
                    colors = listOf(Color(0xFF141312), Color(0xFF1C1A18)),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f),
                )
                PaperThemeMode.AMOLED -> Brush.linearGradient(
                    colors = listOf(Color(0xFF000000), Color(0xFF000000)),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f),
                )
                PaperThemeMode.SYSTEM -> if (systemDark) {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF141312), Color(0xFF1C1A18)),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f),
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFF6F1E7), Color(0xFFEFE8DA)),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f),
                    )
                }
            }

            fun changeThemeMode(newMode: Int, offset: Offset) {
                if (themeMode == newMode) return
                val bitmap = createBitmap(
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
                    themeMode = newMode
                    prefs.edit { putInt("theme_mode", newMode) }

                    val maxRadius = hypot(view.width.toFloat(), view.height.toFloat())
                    transitionRadius.animateTo(maxRadius, animationSpec = tween(700))
                    screenshotState.value = null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
            ) {
                if (screenshotState.value != null) {
                    Image(
                        bitmap = screenshotState.value!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                PaperTheme(mode = paperMode) {
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
                        color = Color.Transparent,
                        contentColor = PaperTheme.colors.textPrimary
                    ) {
                        SongApp(
                            songRepository = SongRepository(context),
                            recentsRepository = RecentsRepository(context),
                            favoritesRepository = FavoritesRepository(context),
                            isDarkTheme = useDarkTheme,
                            themeMode = themeMode,
                            onThemeCycle = { offset ->
                                val nextMode = (themeMode + 1) % 4
                                changeThemeMode(nextMode, offset)
                            },
                            onThemeSelect = { mode, offset ->
                                changeThemeMode(mode, offset)
                            }
                        )
                    }
                }
            }
        }
    }
}
