package com.fluxmusic.player.domain.model

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: Uri?,
    val trackCount: Int,
    val year: Int?
)