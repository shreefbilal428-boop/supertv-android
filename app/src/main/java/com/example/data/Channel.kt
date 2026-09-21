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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val urlsMap = mapOf(
        "Pakistan" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/pk.m3u",
        "India / Bollywood" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in.m3u",
        "Turkey" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/tr.m3u",
        "USA / International" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/us.m3u",
        "Cartoons" to "https://raw.githubusercontent.com/iptv-org/iptv/master/categories/animation.m3u",
        "Chinese Hindi Dubbed" to "https://raw.githubusercontent.com/iptv-org/iptv/master/categories/movies.m3u",
        "Korean Hindi Dubbed" to "https://raw.githubusercontent.com/iptv-org/iptv/master/categories/entertainment.m3u"
    )

    private val richFallbacks = mapOf(
        "Pakistan" to listOf(
            Channel("pk_fb_1", "PTV Home", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_2", "PTV News", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_3", "Dunya News", "https://live.dunyanews.tv/dunya/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/f/fa/Dunya_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_4", "Express News", "https://live.expressnews.tv/express/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/8/8e/Express_News_Logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_5", "PTV Sports", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE")
        ),
        "India / Bollywood" to listOf(
            Channel("in_fb_1", "Aaj Tak", "https://vidgyor.com/aajtak/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Aaj_Tak_logo.png", "India / Bollywood", "India", "LIVE"),
            Channel("in_fb_2", "NDTV 24x7", "https://ndtv24x7.live-s.cdn.bitgravity.com/cdn/ndtv24x7/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/a/ac/NDTV_24x7_logo.png", "India / Bollywood", "India", "LIVE")
        ),
        "Turkey" to listOf(
            Channel("tr_fb_1", "TRT World", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/7/7b/TRT_World_logo.png", "Turkey", "Turkey", "LIVE")
        ),
        "Chinese Hindi Dubbed" to listOf(
            Channel("dub_c_1", "Chinese Action Movie", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300", "Chinese Hindi Dubbed", "China", "MOVIE")
        ),
        "Korean Hindi Dubbed" to listOf(
            Channel("dub_k_1", "Korean Romantic Drama", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300", "Korean Hindi Dubbed", "Korea", "SERIES")
        ),
        "USA / International" to listOf(
            Channel("us_fb_1", "Bloomberg TV", "https://live.bloomberg.com/android/master.m3u8", "https://upload.wikimedia.org/wikipedia/commons/d/ed/Bloomberg_Television_logo.svg", "USA / International", "USA", "LIVE")
        ),
        "Cartoons" to listOf(
            Channel("cart_fb_1", "Kids Cartoon 24/7", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1563089145-599997674d42?w=300", "Cartoons", "Cartoons", "LIVE")
        )
    )

    suspend fun fetchAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val deferredResults = urlsMap.map { (category, url) ->
            async {
                fetchChannelsForCategory(category, url)
            }
        }

        val parsedLists = deferredResults.awaitAll().flatten()
        if (parsedLists.isEmpty()) {
            richFallbacks.values.flatten()
        } else {
            parsedLists
        }
    }

    private suspend fun fetchChannelsForCategory(category: String, url: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val lines = body.lines()
                    var currentName = ""
                    var currentLogo = ""

                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("#EXTINF:")) {
                            currentLogo = extractAttribute(trimmed, "tvg-logo") ?: ""
                            val commaIndex = trimmed.lastIndexOf(',')
                            if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                                currentName = trimmed.substring(commaIndex + 1).trim()
                            }
                        } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            if (currentName.isEmpty()) {
                                currentName = "$category Channel ${channels.size + 1}"
                            }

                            // Clean channel name: remove tags like [720p], [HLS], etc.
                            val cleanName = currentName
                                .replace(Regex("\\[.*?\\]"), "")
                                .replace(Regex("\\(.*?\\)"), "")
                                .trim()
                                .ifEmpty { "$category Channel ${channels.size + 1}" }

                            if (category == "Pakistan") {
                                val nameLower = cleanName.lowercase()
                                val isBlacklisted = nameLower.contains("geo") ||
                                        nameLower.contains("hum") ||
                                        nameLower.contains("ary") ||
                                        nameLower.contains("green")
                                if (isBlacklisted) {
                                    currentName = ""
                                    currentLogo = ""
                                    continue
                                }
                            }

                            channels.add(
                                Channel(
                                    id = "${category.take(3)}_${System.currentTimeMillis()}_${channels.size}",
                                    name = cleanName,
                                    streamUrl = trimmed,
                                    logoUrl = currentLogo,
                                    category = category,
                                    country = category,
                                    contentType = if (cleanName.lowercase().contains("movie")) "MOVIE" else "LIVE"
                                )
                            )
                            currentName = ""
                            currentLogo = ""
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching $category from $url: ${e.message}")
        }

        val fallbacks = richFallbacks[category] ?: emptyList()
        val combined = channels + fallbacks
        // STRICT DUPLICATE REMOVAL (.distinctBy)
        return combined.distinctBy { it.name.lowercase().trim() }
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"([^\"]*)\""
        val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(line)
        return match?.groups?.get(1)?.value
    }
}
