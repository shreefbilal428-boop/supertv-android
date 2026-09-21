package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val category: String,
    val country: String,
    val contentType: String = "LIVE"
)

object ChannelRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val sourceUrls = mapOf(
        "Pakistan" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/pk.m3u",
        "India" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in.m3u",
        "Turkey" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/tr.m3u",
        "Cartoons" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/animation.m3u",
        "News" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/news.m3u",
        "Sports" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/sports.m3u"
    )

    // Fallback static channels in case network fetching fails
    val fallbackChannels = listOf(
        Channel("pk_1", "Geo News", "https://live.geo.tv/georaw/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/e/e4/Geo_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("pk_2", "ARY News", "https://live-arynews.live-stream.com.pk/arynews/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/3/36/ARY_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("pk_3", "Hum News", "https://live-humnews.live-stream.com.pk/humnews/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/d/d3/Hum_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("pk_4", "PTV Sports", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Sports", "Pakistan", "LIVE"),
        Channel("in_1", "Aaj Tak", "https://vidgyor.com/aajtak/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Aaj_Tak_logo.png", "News", "India", "LIVE"),
        Channel("tr_1", "TRT World", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/7/7b/TRT_World_logo.png", "News", "Turkey", "LIVE"),
        Channel("cart_1", "Big Buck Bunny", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "https://images.unsplash.com/photo-1563089145-599997674d42?w=300", "Animation", "Cartoons", "LIVE")
    )

    suspend fun fetchAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val parsedChannels = mutableListOf<Channel>()
        try {
            val deferreds = sourceUrls.map { (category, url) ->
                async {
                    fetchAndParseM3U(url, category)
                }
            }
            val results = deferreds.awaitAll()
            results.forEach { parsedChannels.addAll(it) }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching M3U sources: ${e.message}")
        }

        if (parsedChannels.isEmpty()) {
            fallbackChannels
        } else {
            parsedChannels
        }
    }

    private suspend fun fetchAndParseM3U(m3uUrl: String, defaultCategory: String): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        try {
            val request = Request.Builder()
                .url(m3uUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext emptyList()
                val lines = body.lines()
                var currentName = ""
                var currentLogo = ""
                var currentGroup = defaultCategory

                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#EXTINF:")) {
                        // Extract tvg-logo
                        currentLogo = extractAttribute(trimmed, "tvg-logo") ?: ""
                        // Extract group-title
                        val group = extractAttribute(trimmed, "group-title")
                        if (!group.isNullOrEmpty()) {
                            currentGroup = group
                        }
                        // Extract channel name (after last comma)
                        val commaIndex = trimmed.lastIndexOf(',')
                        if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                            currentName = trimmed.substring(commaIndex + 1).trim()
                        }
                    } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        // This is the stream URL
                        if (currentName.isEmpty()) {
                            currentName = "Channel ${channels.size + 1}"
                        }
                        val channelId = "ch_${System.currentTimeMillis()}_${channels.size}"
                        val country = when {
                            m3uUrl.contains("pk.m3u") -> "Pakistan"
                            m3uUrl.contains("in.m3u") -> "India"
                            m3uUrl.contains("tr.m3u") -> "Turkey"
                            m3uUrl.contains("animation.m3u") -> "Cartoons"
                            m3uUrl.contains("sports.m3u") -> "Sports"
                            else -> "News"
                        }
                        channels.add(
                            Channel(
                                id = channelId,
                                name = currentName,
                                streamUrl = trimmed,
                                logoUrl = currentLogo,
                                category = currentGroup,
                                country = country,
                                contentType = if (m3uUrl.contains("animation")) "ANIMATION" else "LIVE"
                            )
                        )
                        currentName = ""
                        currentLogo = ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Failed to parse $m3uUrl: ${e.message}")
        }
        channels
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"([^\"]*)\""
        val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(line)
        return match?.groups?.get(1)?.value
    }

    suspend fun verifyChannelHealth(channel: Channel): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(channel.streamUrl)
                .head()
                .header("User-Agent", "VLC/3.0.16 LibVLC/3.0.16")
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful || response.code in 200..399
        } catch (e: Exception) {
            false // Dead link
        }
    }
}

object SampleData {
    val channels = ChannelRepository.fallbackChannels
    val musicChannels = ChannelRepository.fallbackChannels
}
