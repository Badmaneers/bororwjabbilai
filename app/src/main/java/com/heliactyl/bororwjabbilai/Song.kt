package com.heliactyl.bororwjabbilai

import com.google.gson.annotations.SerializedName

data class Song(
    @SerializedName("_id") val id: Int,
    @SerializedName("char") val categoryChar: String,
    @SerializedName("suggest_text_1") val title: String,
    @SerializedName("suggest_text_def") val contentHtml: String,
    @SerializedName("suggest_intent_data_id") val intentId: String,
    @SerializedName("favorite") val isFavorite: String
)
