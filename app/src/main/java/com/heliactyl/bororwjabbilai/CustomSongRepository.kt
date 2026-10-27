package com.heliactyl.bororwjabbilai

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class CustomSongRepository(context: Context) {
    private val gson = Gson()
    private val fileName = "custom_songs.json"
    private val file = File(context.filesDir, fileName)

    @Volatile
    private var cachedCustomSongs: List<Song>? = null

    fun getCustomSongs(): List<Song> {
        cachedCustomSongs?.let { return it }
        if (!file.exists()) {
            cachedCustomSongs = emptyList()
            return emptyList()
        }
        return try {
            val listType = object : TypeToken<List<Song>>() {}.type
            val songs: List<Song> = file.reader().use { reader ->
                gson.fromJson(reader, listType) ?: emptyList()
            }
            cachedCustomSongs = songs
            songs
        } catch (_: Exception) {
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
        cachedCustomSongs = currentSongs
        file.writer().use { writer ->
            gson.toJson(currentSongs, writer)
        }
    }

    fun deleteSong(id: Int) {
        val currentSongs = getCustomSongs().filter { it.id != id }
        cachedCustomSongs = currentSongs
        file.writer().use { writer ->
            gson.toJson(currentSongs, writer)
        }
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
                id = generateNewId(),
                isCustom = true,
            )
        } catch (_: Exception) {
            null
        }
    }
}
