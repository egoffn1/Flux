package com.fluxmusic.player.domain.usecases

import android.net.Uri
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.FavoritesRepository
import com.fluxmusic.player.network.SkySoundApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class GetSkySoundRecommendationsUseCase @Inject constructor(
    private val skySoundApi: SkySoundApi,
    private val favoritesRepository: FavoritesRepository
) {
    suspend operator fun invoke(limit: Int = 20): List<Track> {
        val favorites = try {
            favoritesRepository.getFavoriteTracks().first()
        } catch (_: Exception) {
            return emptyList()
        }
        if (favorites.isEmpty()) return emptyList()

        val favoriteIds = favorites.map { it.id }.toSet()
        val artists = favorites.map { it.artist }.filter { it.isNotBlank() }.distinct().take(3)
        if (artists.isEmpty()) return emptyList()

        val allResults = mutableSetOf<Pair<String, String>>()

        for (artist in artists) {
            try {
                val query = "$artist music"
                val results = withTimeout(10_000) { skySoundApi.search(query) }
                for (sr in results) {
                    if (sr.streamUrl.isNotBlank()) {
                        allResults.add(sr.artist to sr.title)
                    }
                }
            } catch (_: Exception) { }
            if (allResults.size >= limit * 2) break
        }

        val tracks = mutableListOf<Track>()
        var index = 0L

        coroutineScope {
            val deferred = allResults.shuffled().take(limit * 2).map { (artist, title) ->
                async {
                    try {
                        val searchResults = withTimeout(10_000) { skySoundApi.search("$artist $title") }
                        searchResults.firstOrNull { it.title.contains(title, ignoreCase = true) && it.artist.contains(artist, ignoreCase = true) }
                    } catch (_: Exception) { null }
                }
            }
            for (deferredResult in deferred) {
                val sr = deferredResult.await() ?: continue
                if (index >= limit) break
                val url = try {
                    withTimeout(10_000) { skySoundApi.getDownloadUrl(sr.downloadPage) }
                } catch (_: Exception) { null }
                val playableUrl = url ?: sr.streamUrl
                if (playableUrl.isNotBlank()) {
                    val durationMs = parseDuration(sr.duration)
                    tracks.add(
                        Track(
                            id = -(System.nanoTime() and 0x7FFFFFFFFFFFFFFF) - index,
                            title = sr.title,
                            artist = sr.artist,
                            album = "SkySound",
                            albumId = 0L,
                            duration = durationMs,
                            uri = Uri.parse(playableUrl),
                            albumArtUri = null,
                            dateAdded = System.currentTimeMillis() / 1000
                        )
                    )
                    index++
                }
            }
        }

        return tracks
    }

    private fun parseDuration(duration: String): Long {
        if (duration.isBlank()) return 0L
        return try {
            val parts = duration.split(":")
            when (parts.size) {
                2 -> parts[0].toLongOrNull()?.let { m ->
                    parts[1].toLongOrNull()?.let { s -> m * 60_000 + s * 1000 }
                }
                3 -> parts[0].toLongOrNull()?.let { h ->
                    parts[1].toLongOrNull()?.let { m ->
                        parts[2].toLongOrNull()?.let { s -> h * 3600_000 + m * 60_000 + s * 1000 }
                    }
                }
                else -> 0L
            } ?: 0L
        } catch (_: Exception) { 0L }
    }
}
