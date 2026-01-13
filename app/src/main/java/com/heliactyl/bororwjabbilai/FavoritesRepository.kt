package com.heliactyl.bororwjabbilai

import android.content.Context
import android.content.SharedPreferences

class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    private val KEY_FAVORITES = "favorite_song_ids"

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
        val string = prefs.getString(KEY_FAVORITES, "") ?: ""
        if (string.isBlank()) return emptyList()
        return try {
            string.split(",").map { it.toInt() }
        } catch (e: NumberFormatException) {
            emptyList()
        }
    }

    private fun saveFavorites(ids: List<Int>) {
        prefs.edit().putString(KEY_FAVORITES, ids.joinToString(",")).apply()
    }
}
