package com.heliactyl.bororwjabbilai

import android.content.Context
import android.content.SharedPreferences

class RecentsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("recents_prefs", Context.MODE_PRIVATE)
    private val KEY_RECENTS = "recent_song_ids"
    private val MAX_RECENTS = 20

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
        val string = prefs.getString(KEY_RECENTS, "") ?: ""
        if (string.isBlank()) return emptyList()
        return try {
            string.split(",").map { it.toInt() }
        } catch (e: NumberFormatException) {
            emptyList()
        }
    }

    private fun saveRecents(ids: List<Int>) {
        prefs.edit().putString(KEY_RECENTS, ids.joinToString(",")).apply()
    }
}
