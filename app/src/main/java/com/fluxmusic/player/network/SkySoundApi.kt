package com.fluxmusic.player.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkySoundApi @Inject constructor() {

    private val base = "https://skysound7.com"

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "$base/api/search?query=$encoded"
            val searchJson = httpGet(searchUrl)
            val url = JSONObject(searchJson).optString("url", "")
            if (url.isEmpty()) return@withContext emptyList()

            val pageJson = httpGet(url)
            val html = JSONObject(pageJson).optString("xx1_content", "")
            if (html.isEmpty()) return@withContext emptyList()

            parseSearchResults(html)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDownloadUrl(downloadPage: String): String? = withContext(Dispatchers.IO) {
        try {
            val json = httpGet(downloadPage)
            val html = JSONObject(json).optString("xx1_content", "")
            if (html.isEmpty()) return@withContext null

            val regex = Regex("""href="(https://fine\.sunproxy\.net/file/[^"]*)"""")
            regex.find(html)?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSearchResults(html: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val trackRegex = Regex(
            """<li class="__adv_list_track">.*?</li>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val matches = trackRegex.findAll(html)
        for (match in matches) {
            val item = match.value
            val artist = extractText(item, """playlist-name-artist[^>]*>.*?<a[^>]*>([^<]*)</a>""")
            val title = extractText(item, """playlist-name-title[^>]*>.*?<em>([^<]*)</em>""")
            val duration = extractText(item, """playlist-duration[^>]*>([^<]*)""")
            val streamUrl = extractAttr(item, """playlist-play[^>]*data-url="([^"]*)""")
            val downPage = extractAttr2(item)
            results.add(SearchResult(artist, title, duration, streamUrl, downPage))
        }
        return results
    }

    private fun extractAttr2(html: String): String {
        val regex = Regex("""<a[^>]*href="([^"]*)"[^>]*class="[^"]*playlist-down[^"]*"""")
        val result = regex.find(html)?.groupValues?.getOrNull(1)?.trim()
        if (!result.isNullOrEmpty()) return result
        val regex2 = Regex("""class="[^"]*playlist-down[^"]*"[^>]*href="([^"]*)"""")
        return regex2.find(html)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractText(html: String, pattern: String): String {
        val regex = Regex(pattern, RegexOption.DOT_MATCHES_ALL)
        return regex.find(html)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractAttr(html: String, pattern: String): String {
        val regex = Regex(pattern)
        return regex.find(html)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        return try {
            BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).readText()
        } finally {
            conn.disconnect()
        }
    }
}
