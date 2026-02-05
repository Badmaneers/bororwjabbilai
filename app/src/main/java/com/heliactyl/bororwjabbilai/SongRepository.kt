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
    fun getSongs(): List<Song> {
        val jsonString: String
        try {
            jsonString = context.assets.open("song.json").bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return emptyList()
        }
        
        try {
            val listType = object : TypeToken<List<Song>>() {}.type
            return Gson().fromJson(jsonString, listType)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            // Extract meaningful part of the error if possible, or show full message
            // Gson error format is usually: com.google.gson.JsonSyntaxException: ... at line X column Y path ...
            val errorMessage = "JSON Error: ${e.localizedMessage}"
            
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            return emptyList()
        }
    }
}
