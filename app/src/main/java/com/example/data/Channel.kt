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

    val priorityChannels = listOf(
        // 1. Pakistan
        Channel("pak_1", "Hum TV", "https://live-humtv.live-stream.com.pk/humtv/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/d/d3/Hum_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_2", "ARY Digital", "https://live-arydigital.live-stream.com.pk/arydigital/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/3/36/ARY_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_3", "Geo Entertainment", "https://live.geo.tv/georaw/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/e/e4/Geo_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_4", "Green TV", "https://greentv-live.ercdn.net/greentv/greentv.m3u8", "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=300", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_5", "Geo News", "https://live.geo.tv/georaw/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/e/e4/Geo_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_6", "ARY News", "https://live-arynews.live-stream.com.pk/arynews/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/3/36/ARY_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_7", "Hum News", "https://live-humnews.live-stream.com.pk/humnews/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/d/d3/Hum_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_8", "Dunya News", "https://live.dunyanews.tv/dunya/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/f/fa/Dunya_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_9", "Express News", "https://live.expressnews.tv/express/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/8/8e/Express_News_Logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_10", "PTV Sports", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),

        // 2. India / Bollywood
        Channel("in_1", "Aaj Tak", "https://vidgyor.com/aajtak/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Aaj_Tak_logo.png", "India / Bollywood", "India", "LIVE"),
        Channel("in_2", "NDTV 24x7", "https://ndtv24x7.live-s.cdn.bitgravity.com/cdn/ndtv24x7/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/a/ac/NDTV_24x7_logo.png", "India / Bollywood", "India", "LIVE"),

        // 3. Turkey
        Channel("tr_1", "TRT World", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/7/7b/TRT_World_logo.png", "Turkey", "Turkey", "LIVE"),

        // 4. Chinese Hindi Dubbed
        Channel("dub_c_1", "Chinese Action Movie (Hindi Dubbed)", "https://live.geo.tv/georaw/index.m3u8", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300", "Chinese Hindi Dubbed", "China", "MOVIE"),

        // 5. Korean Hindi Dubbed
        Channel("dub_k_1", "Korean Romantic Drama (Hindi Dubbed)", "https://live-humtv.live-stream.com.pk/humtv/index.m3u8", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300", "Korean Hindi Dubbed", "Korea", "SERIES"),

        // 6. USA / International
        Channel("intl_1", "Bloomberg TV", "https://live.bloomberg.com/android/master.m3u8", "https://upload.wikimedia.org/wikipedia/commons/d/ed/Bloomberg_Television_logo.svg", "USA / International", "USA", "LIVE"),

        // 7. Cartoons
        Channel("cart_1", "Kids Cartoon 24/7", "https://live-arydigital.live-stream.com.pk/arydigital/index.m3u8", "https://images.unsplash.com/photo-1563089145-599997674d42?w=300", "Cartoons", "Cartoons", "LIVE")
    )

    val sourceUrls = mapOf(
        "Pakistan" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/pk.m3u",
        "India / Bollywood" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in.m3u",
        "Turkey" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/tr.m3u",
        "USA / International" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/us.m3u",
        "Cartoons" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/animation.m3u"
    )

    suspend fun fetchAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val parsedChannels = mutableListOf<Channel>()
        parsedChannels.addAll(priorityChannels)

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

        parsedChannels
    }

    private suspend fun fetchAndParseM3U(m3uUrl: String, defaultCategory: String): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        try {
            val request = Request.Builder()
                .url(m3uUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
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
                        currentLogo = extractAttribute(trimmed, "tvg-logo") ?: ""
                        val group = extractAttribute(trimmed, "group-title")
                        if (!group.isNullOrEmpty()) {
                            currentGroup = group
                        }
                        val commaIndex = trimmed.lastIndexOf(',')
                        if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                            currentName = trimmed.substring(commaIndex + 1).trim()
                        }
                    } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        if (currentName.isEmpty()) {
                            currentName = "Channel ${channels.size + 1}"
                        }
                        val channelId = "ch_${System.currentTimeMillis()}_${channels.size}"
                        val country = when {
                            m3uUrl.contains("pk.m3u") -> "Pakistan"
                            m3uUrl.contains("in.m3u") -> "India / Bollywood"
                            m3uUrl.contains("tr.m3u") -> "Turkey"
                            m3uUrl.contains("us.m3u") -> "USA / International"
                            m3uUrl.contains("animation.m3u") -> "Cartoons"
                            else -> defaultCategory
                        }
                        channels.add(
                            Channel(
                                id = channelId,
                                name = currentName,
                                streamUrl = trimmed,
                                logoUrl = currentLogo,
                                category = country,
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
}

object SampleData {
    val channels = ChannelRepository.priorityChannels
    val musicChannels = ChannelRepository.priorityChannels
}
