package com.heliactyl.bororwjabbilai

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.IOException

class SongRepository(private val context: Context) {
    @Volatile
    private var cachedSongs: List<Song>? = null

    fun getSongs(): List<Song> {
        cachedSongs?.let { return it }

        val jsonString = try {
            context.assets.open("song.json").bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return emptyList()
        }
        
        return try {
            val listType = object : TypeToken<List<Song>>() {}.type
            val songs: List<Song> = Gson().fromJson(jsonString, listType) ?: emptyList()
            cachedSongs = songs
            songs
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            val errorMessage = "JSON Error: ${e.localizedMessage}"
            
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            emptyList()
        }
    }
}
