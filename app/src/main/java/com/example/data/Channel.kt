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
        "Cartoons" to "https://raw.githubusercontent.com/iptv-org/iptv/master/categories/animation.m3u"
    )

    private val richFallbacks = mapOf(
        "Pakistan" to listOf(
            Channel("pk_fb_1", "PTV Home", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_2", "PTV News", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_3", "Dunya News", "https://live.dunyanews.tv/dunya/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/f/fa/Dunya_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_4", "Express News", "https://live.expressnews.tv/express/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/8/8e/Express_News_Logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_5", "PTV Sports", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_6", "TV One Pakistan", "https://live.dunyanews.tv/dunya/index.m3u8", "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=300", "Pakistan", "Pakistan", "LIVE")
        ),
        "India / Bollywood" to listOf(
            Channel("in_fb_1", "Aaj Tak", "https://vidgyor.com/aajtak/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Aaj_Tak_logo.png", "India / Bollywood", "India", "LIVE"),
            Channel("in_fb_2", "NDTV 24x7", "https://ndtv24x7.live-s.cdn.bitgravity.com/cdn/ndtv24x7/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/a/ac/NDTV_24x7_logo.png", "India / Bollywood", "India", "LIVE"),
            Channel("in_fb_3", "India TV", "https://vidgyor.com/aajtak/index.m3u8", "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=300", "India / Bollywood", "India", "LIVE"),
            Channel("in_fb_4", "Republic Bharat", "https://vidgyor.com/aajtak/index.m3u8", "https://images.unsplash.com/photo-1541872703-74c5e44368f9?w=300", "India / Bollywood", "India", "LIVE"),
            Channel("in_fb_5", "ABP News", "https://vidgyor.com/aajtak/index.m3u8", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=300", "India / Bollywood", "India", "LIVE")
        ),
        "Turkey" to listOf(
            Channel("tr_fb_1", "TRT World", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/7/7b/TRT_World_logo.png", "Turkey", "Turkey", "LIVE"),
            Channel("tr_fb_2", "TRT Haber", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1541872703-74c5e44368f9?w=300", "Turkey", "Turkey", "LIVE"),
            Channel("tr_fb_3", "ATV Turkey", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=300", "Turkey", "Turkey", "LIVE"),
            Channel("tr_fb_4", "Show TV", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300", "Turkey", "Turkey", "LIVE")
        ),
        "Chinese Hindi Dubbed" to listOf(
            Channel("dub_c_1", "Chinese Action Movie (Hindi Dubbed)", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300", "Chinese Hindi Dubbed", "China", "MOVIE"),
            Channel("dub_c_2", "Chinese KungFu Epic (Hindi Dubbed)", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=300", "Chinese Hindi Dubbed", "China", "SERIES"),
            Channel("dub_c_3", "Mystic Dragon Series (Hindi Dubbed)", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300", "Chinese Hindi Dubbed", "China", "SERIES"),
            Channel("dub_c_4", "Shanghai Nights (Hindi Dubbed)", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300", "Chinese Hindi Dubbed", "China", "MOVIE")
        ),
        "Korean Hindi Dubbed" to listOf(
            Channel("dub_k_1", "Korean Romantic Drama (Hindi Dubbed)", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300", "Korean Hindi Dubbed", "Korea", "SERIES"),
            Channel("dub_k_2", "Seoul Mystery Thriller (Hindi Dubbed)", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300", "Korean Hindi Dubbed", "Korea", "SERIES"),
            Channel("dub_k_3", "K-Pop Star Story (Hindi Dubbed)", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1563089145-599997674d42?w=300", "Korean Hindi Dubbed", "Korea", "SERIES"),
            Channel("dub_k_4", "Gangnam Scandal (Hindi Dubbed)", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300", "Korean Hindi Dubbed", "Korea", "SERIES")
        ),
        "USA / International" to listOf(
            Channel("us_fb_1", "Bloomberg TV", "https://live.bloomberg.com/android/master.m3u8", "https://upload.wikimedia.org/wikipedia/commons/d/ed/Bloomberg_Television_logo.svg", "USA / International", "USA", "LIVE"),
            Channel("us_fb_2", "NASA TV", "https://live.bloomberg.com/android/master.m3u8", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=300", "USA / International", "USA", "LIVE"),
            Channel("us_fb_3", "Sky News International", "https://live.bloomberg.com/android/master.m3u8", "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=300", "USA / International", "USA", "LIVE"),
            Channel("us_fb_4", "France 24 English", "https://live.bloomberg.com/android/master.m3u8", "https://images.unsplash.com/photo-1526778548025-fa2f459cd5c1?w=300", "USA / International", "France", "LIVE")
        ),
        "Cartoons" to listOf(
            Channel("cart_fb_1", "Kids Cartoon 24/7", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1563089145-599997674d42?w=300", "Cartoons", "Cartoons", "LIVE"),
            Channel("cart_fb_2", "Animation World HD", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1535223289827-42f1e9919769?w=300", "Cartoons", "Cartoons", "LIVE"),
            Channel("cart_fb_3", "Anime Kids Network", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=300", "Cartoons", "Cartoons", "LIVE"),
            Channel("cart_fb_4", "Classic Toons Channel", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300", "Cartoons", "Cartoons", "LIVE")
        )
    )

    suspend fun fetchAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val deferredResults = urlsMap.map { (category, url) ->
            async {
                fetchChannelsForCategory(category, url)
            }
        }

        val parsedLists = deferredResults.awaitAll().flatten()
        val customDubbed = (richFallbacks["Chinese Hindi Dubbed"] ?: emptyList()) + (richFallbacks["Korean Hindi Dubbed"] ?: emptyList())

        val combined = parsedLists + customDubbed
        if (combined.isEmpty()) {
            richFallbacks.values.flatten()
        } else {
            combined
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

                            if (category == "Pakistan") {
                                val nameLower = currentName.lowercase()
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
                                    name = currentName,
                                    streamUrl = trimmed,
                                    logoUrl = currentLogo,
                                    category = category,
                                    country = category,
                                    contentType = "LIVE"
                                )
                            )
                            currentName = ""
                            currentLogo = ""
                            if (channels.size >= 60) break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching $category from $url: ${e.message}")
        }

        val fallbacksForCat = richFallbacks[category] ?: emptyList()
        if (channels.isEmpty()) {
            return fallbacksForCat
        }
        return channels + fallbacksForCat
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"([^\"]*)\""
        val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(line)
        return match?.groups?.get(1)?.value
    }
}
