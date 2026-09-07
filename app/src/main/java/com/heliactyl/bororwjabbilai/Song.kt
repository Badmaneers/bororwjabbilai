package com.heliactyl.bororwjabbilai

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Keep
@Immutable
data class Song(
    @SerializedName("id") val id: Int,
    @SerializedName("category") val categoryChar: String,
    @SerializedName("title") val title: String,
    @SerializedName("lyrics") val lyrics: List<LyricSection> = emptyList(),
    @SerializedName("isFavorite") val isFavorite: Boolean = false,
    @SerializedName("isCustom") val isCustom: Boolean = false,
) {
    val searchLyricsText: String by lazy {
        lyrics.asSequence().flatMap { it.lines }.joinToString(" ")
    }

    val searchIdString: String by lazy {
        id.toString()
    }

    val searchTitleLower: String by lazy {
        title.lowercase()
    }
}

@Keep
@Immutable
data class LyricSection(
    @SerializedName("type") val type: String,
    @SerializedName("number") val number: Int? = null,
    @SerializedName("lines") val lines: List<String> = emptyList(),
)
