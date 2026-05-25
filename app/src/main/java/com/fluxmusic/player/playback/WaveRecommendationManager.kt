package com.fluxmusic.player.playback

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.Player
import com.fluxmusic.player.data.scanner.MediaScanner
import com.fluxmusic.player.domain.model.Track
import com.fluxmusic.player.domain.repository.FavoritesRepository
import com.fluxmusic.player.domain.repository.MusicRepository
import com.fluxmusic.player.network.SkySoundApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaveRecommendationManager @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val musicRepository: MusicRepository,
    private val skySoundApi: SkySoundApi,
    private val mediaSessionConnection: MediaSessionConnection,
    private val queueManager: QueueManager
) {
    companion object {
        private const val TAG = "FluxWave"
        private const val INITIAL_BATCH = 5
        private const val LOAD_MORE_COUNT = 5
        private const val PRELOAD_THRESHOLD = 2
        private const val RECENT_MEMORY = 25

        private val SUFFIXES = arrayOf(
            "remix", "edit", "version", "extended", "radio edit", "album version",
            "slowed", "reverb", "sped up", "nightcore", "daycore",
            "mashup", "bootleg", "flip", "refix", "VIP", "VIP mix",
            "instrumental", "a cappella", "acoustic", "live",
            "original mix", "club mix", "dub mix", "dubstep remix",
            "trap remix", "house remix", "chill remix", "lofi remix",
            "bass boosted", "phonk remix", "drift phonk",
            "super slowed", "ultra slowed",
            "Extended Edit Version", "Slowed _ Reverb", "Slowed & Reverb"
        )

        private val SUFFIX_REGEX = Regex(
            SUFFIXES
                .map { Regex.escape(it) }
                .joinToString("|")
                .let { "(?i)[\\s\\-\\[(|]*($it)[\\s\\]|)]*$" }
        )

        private val BRACKET_CONTENT = Regex("""\s*[\[\(].*?[\]\)]\s*""")

        private val SEPARATOR = Regex("""\s+[x×X╳✕✖]\s+""")

        private val FEAT_PATTERN = Regex("""(?i)\s+feat\.?\s+""")

        private val PAREN_CONTENT_END = Regex("""\s*\([^)]*\)\s*$""")
    }

    private val _isMyWaveActive = MutableStateFlow(false)
    val isMyWaveActive: StateFlow<Boolean> = _isMyWaveActive.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private var loadMoreJob: Job? = null
    private var isCurrentlyLoading = false

    private val handler = Handler(Looper.getMainLooper())
    private var playerListener: Player.Listener? = null

    private val recentNormalizedTitles = ArrayDeque<String>(RECENT_MEMORY)

    private val searchQueries = listOf(
        "music", "song", "track", "best songs", "greatest hits",
        "top tracks", "popular"
    )
    private var queryIndex = 0

    fun start() {
        if (_isMyWaveActive.value) return
        _isMyWaveActive.value = true
        isCurrentlyLoading = false
        recentNormalizedTitles.clear()
        queryIndex = 0
        Log.d(TAG, "Wave started")

        val controller = mediaSessionConnection.getMediaController()
        if (controller != null) {
            attachListener(controller)
            loadInitialBatch()
        } else {
            handler.postDelayed({
                val c = mediaSessionConnection.getMediaController()
                if (c != null) {
                    attachListener(c)
                    loadInitialBatch()
                }
            }, 500)
        }
    }

    fun stop() {
        if (!_isMyWaveActive.value) return
        _isMyWaveActive.value = false
        loadMoreJob?.cancel()
        isCurrentlyLoading = false
        Log.d(TAG, "Wave stopped")
    }

    private fun attachListener(controller: androidx.media3.common.Player) {
        if (playerListener != null) return
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val currentIdx = controller.currentMediaItemIndex
                val totalItems = controller.mediaItemCount
                onItemTransition(currentIdx, totalItems)
            }
        }
        controller.addListener(listener)
        playerListener = listener
        Log.d(TAG, "Listener attached")
    }

    private fun onItemTransition(currentIndex: Int, totalItems: Int) {
        if (!_isMyWaveActive.value || isCurrentlyLoading) return
        if (totalItems >= PRELOAD_THRESHOLD && currentIndex >= totalItems - PRELOAD_THRESHOLD) {
            loadMore()
        }
    }

    private fun loadInitialBatch() {
        scope.launch {
            try {
                val tracks = withTimeout(30_000) { loadTracks(INITIAL_BATCH) }
                if (tracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        mediaSessionConnection.playTracks(tracks, 0)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Initial batch failed, fallback to local")
                val fallback = loadLocalFallback(INITIAL_BATCH)
                if (fallback.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        mediaSessionConnection.playTracks(fallback, 0)
                    }
                }
            }
        }
    }

    private fun loadMore() {
        loadMoreJob?.cancel()
        isCurrentlyLoading = true
        loadMoreJob = scope.launch {
            try {
                val tracks = withTimeout(30_000) { loadTracks(LOAD_MORE_COUNT) }
                if (tracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        mediaSessionConnection.addTracksToEnd(tracks)
                    }
                } else {
                    val fallback = loadLocalFallback(LOAD_MORE_COUNT)
                    if (fallback.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            mediaSessionConnection.addTracksToEnd(fallback)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "LoadMore failed, local fallback")
                val fallback = loadLocalFallback(LOAD_MORE_COUNT)
                if (fallback.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        mediaSessionConnection.addTracksToEnd(fallback)
                    }
                }
            }
            isCurrentlyLoading = false
        }
    }

    private suspend fun loadTracks(count: Int): List<Track> {
        val favorites = try {
            favoritesRepository.getFavoriteTracks().first()
        } catch (_: Exception) { emptyList() }

        val artists = favorites
            .map { it.artist }
            .filter { it.isNotBlank() && !MediaScanner.isUnknownArtist(it) }
            .distinct()
            .take(3)

        if (artists.isEmpty()) return loadLocalFallback(count)

        val allCandidates = mutableListOf<Pair<Track, String>>()

        val shuffledArtists = artists.shuffled()
        for (artist in shuffledArtists) {
            val query = "$artist ${searchQueries[queryIndex % searchQueries.size]}"
            queryIndex++
            try {
                val searchResults = withTimeout(10_000) {
                    skySoundApi.search(query)
                }
                for (sr in searchResults.shuffled().take(15)) {
                    val url = try {
                        withTimeout(8_000) { skySoundApi.getDownloadUrl(sr.downloadPage) }
                    } catch (_: Exception) { null }
                    val playableUrl = url ?: sr.streamUrl.takeIf { it.isNotBlank() } ?: continue
                    val norm = normalizeTitle(sr.title)
                    if (norm.isBlank()) continue
                    val track = Track(
                        id = -(System.nanoTime() and 0x7FFFFFFFFFFFFFFF) - allCandidates.size.toLong(),
                        title = sr.title,
                        artist = sr.artist,
                        album = "SkySound",
                        albumId = 0L,
                        duration = parseDuration(sr.duration),
                        uri = Uri.parse(playableUrl),
                        albumArtUri = null,
                        dateAdded = System.currentTimeMillis() / 1000
                    )
                    allCandidates.add(track to norm)
                }
            } catch (_: Exception) { }
        }

        val selected = mutableListOf<Track>()
        val usedNormals = mutableSetOf<String>()

        val deduped = allCandidates.distinctBy { (_, norm) -> norm }
            .shuffled()

        for ((track, norm) in deduped) {
            if (selected.size >= count) break
            if (norm in recentNormalizedTitles) continue
            if (norm in usedNormals) continue
            selected.add(track)
            usedNormals.add(norm)
            recentNormalizedTitles.addLast(norm)
            if (recentNormalizedTitles.size > RECENT_MEMORY) {
                recentNormalizedTitles.removeFirst()
            }
        }

        if (selected.size < count) {
            val local = loadLocalFallback(count - selected.size, selected)
            selected.addAll(local)
        }

        return selected
    }

    private suspend fun loadLocalFallback(
        count: Int,
        exclude: List<Track> = emptyList()
    ): List<Track> {
        return try {
            val allTracks = musicRepository.getAllTracks().first()
            val excludeUris = exclude.map { it.uri.toString() }.toSet()
            val recentNormals = recentNormalizedTitles.toSet()
            allTracks
                .filter { it.uri.toString() !in excludeUris }
                .filter { normalizeTitle(it.title) !in recentNormals }
                .shuffled()
                .take(count)
        } catch (_: Exception) { emptyList() }
    }

    private fun normalizeTitle(title: String): String {
        var t = title.trim()
        t = t.replace(BRACKET_CONTENT, " ")
        t = t.replace(PAREN_CONTENT_END, "")
        t = t.replace(SEPARATOR, " × ")
        t = t.replace(FEAT_PATTERN, " ")
        t = t.replace(SUFFIX_REGEX, "")
        t = t.replace(Regex("""\s+"""), " ")
        t = t.trim()
        t = t.lowercase()
        return t
    }

    private fun parseDuration(duration: String): Long {
        if (duration.isBlank()) return 0L
        return try {
            val parts = duration.split(":")
            when (parts.size) {
                2 -> (parts[0].toLongOrNull() ?: 0L) * 60_000 + (parts[1].toLongOrNull() ?: 0L) * 1000
                3 -> (parts[0].toLongOrNull() ?: 0L) * 3600_000 + (parts[1].toLongOrNull() ?: 0L) * 60_000 + (parts[2].toLongOrNull() ?: 0L) * 1000
                else -> 0L
            }
        } catch (_: Exception) { 0L }
    }
}
