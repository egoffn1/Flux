package com.fluxmusic.player.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAt: Long
)