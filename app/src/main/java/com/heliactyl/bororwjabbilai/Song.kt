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
    @Transient
    private var _searchLyricsText: String? = null

    val searchLyricsText: String
        get() {
            var text = _searchLyricsText
            if (text == null) {
                text = lyrics.asSequence().flatMap { it.lines }.joinToString(" ")
                _searchLyricsText = text
            }
            return text
        }

    @Transient
    private var _searchIdString: String? = null

    val searchIdString: String
        get() {
            var idStr = _searchIdString
            if (idStr == null) {
                idStr = id.toString()
                _searchIdString = idStr
            }
            return idStr
        }

    @Transient
    private var _searchTitleLower: String? = null

    val searchTitleLower: String
        get() {
            var titleLower = _searchTitleLower
            if (titleLower == null) {
                titleLower = title.lowercase()
                _searchTitleLower = titleLower
            }
            return titleLower
        }
}

@Keep
@Immutable
data class LyricSection(
    @SerializedName("type") val type: String,
    @SerializedName("number") val number: Int? = null,
    @SerializedName("lines") val lines: List<String> = emptyList(),
)
