package com.heliactyl.bororwjabbilai

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class RecentsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("recents_prefs", Context.MODE_PRIVATE)
    
    @Suppress("PrivatePropertyName")
    private val KEY_RECENTS = "recent_song_ids"
    
    @Suppress("PrivatePropertyName")
    private val MAX_RECENTS = 20

    @Volatile
    private var cachedIds: List<Int>? = null

    fun addRecent(songId: Int) {
        val currentRecents = getRecentIds().toMutableList()
        currentRecents.remove(songId) // Remove if exists to move to top
        currentRecents.add(0, songId) // Add to top
        
        if (currentRecents.size > MAX_RECENTS) {
            currentRecents.removeAt(currentRecents.lastIndex)
        }
        
        saveRecents(currentRecents)
    }

    fun getRecentIds(): List<Int> {
        cachedIds?.let { return it }
        val string = prefs.getString(KEY_RECENTS, "") ?: ""
        if (string.isBlank()) {
            cachedIds = emptyList()
            return emptyList()
        }
        return try {
            val ids = string.split(",").mapNotNull { it.toIntOrNull() }
            cachedIds = ids
            ids
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveRecents(ids: List<Int>) {
        cachedIds = ids
        prefs.edit { putString(KEY_RECENTS, ids.joinToString(",")) }
    }
}
