package com.heliactyl.bororwjabbilai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.heliactyl.bororwjabbilai.ui.SongApp
import com.heliactyl.bororwjabbilai.ui.theme.BoroRwjabBilaiTheme

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
            val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
            
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
