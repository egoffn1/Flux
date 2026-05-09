package com.fluxmusic.player.domain.model

data class Artist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int
)