package com.example.albuddy.data.model

import com.google.gson.annotations.SerializedName

data class BackupData(
    @SerializedName("haUrl") val haUrl: String?,
    @SerializedName("haToken") val haToken: String?,
    @SerializedName("playSound") val playSound: Boolean,
    @SerializedName("vibrate") val vibrate: Boolean,
    @SerializedName("commands") val commands: List<Command>
)
