package com.heliactyl.bororwjabbilai

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Song(
    @SerializedName("id") val id: Int,
    @SerializedName("category") val categoryChar: String,
    @SerializedName("title") val title: String,
    @SerializedName("lyrics") val lyrics: List<LyricSection> = emptyList(),
    @SerializedName("intentId") val intentId: String,
    @SerializedName("isFavorite") var isFavorite: Boolean,
    @SerializedName("isCustom") val isCustom: Boolean = false
)

@Keep
data class LyricSection(
    @SerializedName("type") val type: String,
    @SerializedName("number") val number: Int? = null,
    @SerializedName("lines") val lines: List<String> = emptyList()
)

