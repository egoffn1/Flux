package com.fluxmusic.player.network

data class SearchResult(
    val artist: String,
    val title: String,
    val duration: String,
    val streamUrl: String,
    val downloadPage: String
)
