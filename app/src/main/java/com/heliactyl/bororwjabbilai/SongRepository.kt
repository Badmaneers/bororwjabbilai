package com.heliactyl.bororwjabbilai

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class SongRepository(private val context: Context) {
    fun getSongs(): List<Song> {
        val jsonString: String
        try {
            jsonString = context.assets.open("song.json").bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return emptyList()
        }
        val listType = object : TypeToken<List<Song>>() {}.type
        return Gson().fromJson(jsonString, listType)
    }
}
