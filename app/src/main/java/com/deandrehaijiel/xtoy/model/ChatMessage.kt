package com.deandrehaijiel.xtoy.model

import java.util.Date

@Suppress("unused")
data class ChatMessage(
    val documentId: String,
    val fromId: String,
    val toId: String,
    val message: String,
    val timestamp: Date
) {
    constructor() : this("", "", "", "", Date())
}