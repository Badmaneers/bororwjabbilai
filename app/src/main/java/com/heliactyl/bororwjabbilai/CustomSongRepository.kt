package com.heliactyl.bororwjabbilai

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class CustomSongRepository(private val context: Context) {
    private val gson = Gson()
    private val fileName = "custom_songs.json"
    private val file = File(context.filesDir, fileName)

    fun getCustomSongs(): List<Song> {
        if (!file.exists()) return emptyList()
        return try {
            val jsonString = file.readText()
            val listType = object : TypeToken<List<Song>>() {}.type
            gson.fromJson(jsonString, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSong(song: Song) {
        val currentSongs = getCustomSongs().toMutableList()
        val index = currentSongs.indexOfFirst { it.id == song.id }
        if (index != -1) {
            currentSongs[index] = song
        } else {
            currentSongs.add(song)
        }
        file.writeText(gson.toJson(currentSongs))
    }

    fun deleteSong(id: Int) {
        val currentSongs = getCustomSongs().filter { it.id != id }
        file.writeText(gson.toJson(currentSongs))
    }

    fun generateNewId(): Int {
        val customSongs = getCustomSongs()
        val maxId = customSongs.maxOfOrNull { it.id } ?: 739
        return maxOf(maxId + 1, 740)
    }

    fun exportSongAsCode(song: Song): String {
        val json = gson.toJson(song)
        return Base64.encodeToString(json.toByteArray(), Base64.DEFAULT)
    }

    fun importSongFromCode(code: String): Song? {
        return try {
            val decodedBytes = Base64.decode(code, Base64.DEFAULT)
            val json = String(decodedBytes)
            gson.fromJson(json, Song::class.java).copy(
                id = generateNewId(), // Assign a new local ID
                isCustom = true
            )
        } catch (e: Exception) {
            null
        }
    }
}
