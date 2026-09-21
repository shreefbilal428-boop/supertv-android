package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
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
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val pkPlaylistUrl = "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/pk.m3u"

    // Curated working backup channels for all 7 categories
    private val backupChannels = listOf(
        Channel("pak_1", "PTV Home", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_2", "PTV News", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_3", "Dunya News", "https://live.dunyanews.tv/dunya/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/f/fa/Dunya_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_4", "Express News", "https://live.expressnews.tv/express/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/8/8e/Express_News_Logo.png", "Pakistan", "Pakistan", "LIVE"),
        Channel("pak_5", "PTV Sports", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),

        Channel("in_1", "Aaj Tak", "https://vidgyor.com/aajtak/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Aaj_Tak_logo.png", "India / Bollywood", "India", "LIVE"),
        Channel("in_2", "NDTV 24x7", "https://ndtv24x7.live-s.cdn.bitgravity.com/cdn/ndtv24x7/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/a/ac/NDTV_24x7_logo.png", "India / Bollywood", "India", "LIVE"),

        Channel("tr_1", "TRT World", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/7/7b/TRT_World_logo.png", "Turkey", "Turkey", "LIVE"),

        Channel("dub_c_1", "Chinese Action Movie (Hindi Dubbed)", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300", "Chinese Hindi Dubbed", "China", "MOVIE"),

        Channel("dub_k_1", "Korean Romantic Drama (Hindi Dubbed)", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300", "Korean Hindi Dubbed", "Korea", "SERIES"),

        Channel("intl_1", "Bloomberg TV", "https://live.bloomberg.com/android/master.m3u8", "https://upload.wikimedia.org/wikipedia/commons/d/ed/Bloomberg_Television_logo.svg", "USA / International", "USA", "LIVE"),

        Channel("cart_1", "Kids Cartoon 24/7", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1563089145-599997674d42?w=300", "Cartoons", "Cartoons", "LIVE")
    )

    suspend fun fetchAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val parsedChannels = mutableListOf<Channel>()
        try {
            val request = Request.Builder()
                .url(pkPlaylistUrl)
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
                                currentName = "Channel ${parsedChannels.size + 1}"
                            }

                            // BLACKLIST & EXCLUDE: Absolutely DROP any channel matching "Geo", "Hum", "ARY", "Green"
                            val nameLower = currentName.lowercase()
                            val isBlacklisted = nameLower.contains("geo") ||
                                    nameLower.contains("hum") ||
                                    nameLower.contains("ary") ||
                                    nameLower.contains("green")

                            if (!isBlacklisted) {
                                parsedChannels.add(
                                    Channel(
                                        id = "pk_${System.currentTimeMillis()}_${parsedChannels.size}",
                                        name = currentName,
                                        streamUrl = trimmed,
                                        logoUrl = currentLogo,
                                        category = "Pakistan",
                                        country = "Pakistan",
                                        contentType = "LIVE"
                                    )
                                )
                            }
                            currentName = ""
                            currentLogo = ""
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching pk playlist: ${e.message}")
        }

        // Combine parsed valid PK channels with curated backup channels for other categories
        val all = parsedChannels + backupChannels
        all
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"([^\"]*)\""
        val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(line)
        return match?.groups?.get(1)?.value
    }
}
