package com.heliactyl.bororwjabbilai

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    
    @Suppress("PrivatePropertyName")
    private val KEY_FAVORITES = "favorite_song_ids"

    @Volatile
    private var cachedIds: List<Int>? = null

    fun toggleFavorite(songId: Int) {
        val currentFavorites = getFavoriteIds().toMutableList()
        if (currentFavorites.contains(songId)) {
            currentFavorites.remove(songId)
        } else {
            currentFavorites.add(songId)
        }
        saveFavorites(currentFavorites)
    }

    fun isFavorite(songId: Int): Boolean {
        return getFavoriteIds().contains(songId)
    }

    fun getFavoriteIds(): List<Int> {
        cachedIds?.let { return it }
        val string = prefs.getString(KEY_FAVORITES, "") ?: ""
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

    private fun saveFavorites(ids: List<Int>) {
        cachedIds = ids
        prefs.edit { putString(KEY_FAVORITES, ids.joinToString(",")) }
    }
}
