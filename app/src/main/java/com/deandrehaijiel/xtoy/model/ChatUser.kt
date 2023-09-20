package com.deandrehaijiel.xtoy.model

data class ChatUser(
    val xId: String,
    val xEmail: String,
    val xName: String,
    val xProfileImageUrl: String,

    val pair: String,

    val yId: String,
    val yEmail: String,
    val yName: String,
    val yProfileImageUrl: String,
)