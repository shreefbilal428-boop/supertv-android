package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
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
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val urlsMap = mapOf(
        "Pakistan" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/pk.m3u",
        "India / Bollywood" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in.m3u",
        "Turkey" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/tr.m3u",
        "Cartoons" to "https://raw.githubusercontent.com/iptv-org/iptv/master/categories/animation.m3u",
        "Chinese Channels" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/cn.m3u",
        "USA / International" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/us.m3u",
        "Movies Base" to "https://raw.githubusercontent.com/iptv-org/iptv/master/categories/movies.m3u",
        "Series Base" to "https://raw.githubusercontent.com/iptv-org/iptv/master/categories/series.m3u"
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
        "Cartoons" to listOf(
            Channel("cart_1", "Kids Cartoon 24/7 HD", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1563089145-599997674d42?w=300", "Cartoons", "Cartoons", "LIVE")
        ),
        "Bollywood Movies" to listOf(
            Channel("bm_fb_1", "Bollywood Action Cinema", "https://vidgyor.com/aajtak/index.m3u8", "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=300", "Bollywood Movies", "India", "MOVIE"),
            Channel("bm_fb_2", "Goldmines Movies HD", "https://vidgyor.com/aajtak/index.m3u8", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300", "Bollywood Movies", "India", "MOVIE")
        ),
        "Hollywood Movies" to listOf(
            Channel("hm_fb_1", "Hollywood Blockbusters 24/7", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=300", "Hollywood Movies", "USA", "MOVIE"),
            Channel("hm_fb_2", "HBO Hollywood HD", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300", "Hollywood Movies", "USA", "MOVIE")
        ),
        "Tollywood Movies" to listOf(
            Channel("tm_fb_1", "Tollywood South Action Gold", "https://vidgyor.com/aajtak/index.m3u8", "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=300", "Tollywood Movies", "India", "MOVIE"),
            Channel("tm_fb_2", "South Indian Cinema HD", "https://vidgyor.com/aajtak/index.m3u8", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300", "Tollywood Movies", "India", "MOVIE")
        ),
        "Bollywood Web Series" to listOf(
            Channel("bws_fb_1", "Hindi Web Series 24/7", "https://vidgyor.com/aajtak/index.m3u8", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=300", "Bollywood Web Series", "India", "SERIES"),
            Channel("bws_fb_2", "Indian Crime Thrillers Series", "https://vidgyor.com/aajtak/index.m3u8", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300", "Bollywood Web Series", "India", "SERIES")
        ),
        "Hollywood Web Series" to listOf(
            Channel("hws_fb_1", "Hollywood Sci-Fi Series", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300", "Hollywood Web Series", "USA", "SERIES"),
            Channel("hws_fb_2", "US Drama Series HD", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=300", "Hollywood Web Series", "USA", "SERIES")
        )
    )

    suspend fun fetchAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val deferredResults = urlsMap.map { (categoryKey, url) ->
            async {
                fetchAndCategorizeStreamed(categoryKey, url)
            }
        }

        val rawList = deferredResults.awaitAll().flatten()
        val groupedList = mutableListOf<Channel>()

        val categoriesToInclude = listOf(
            "Pakistan",
            "India / Bollywood",
            "Turkey",
            "Cartoons",
            "Chinese Channels",
            "USA / International",
            "Bollywood Movies",
            "Hollywood Movies",
            "Tollywood Movies",
            "Bollywood Web Series",
            "Hollywood Web Series"
        )

        for (cat in categoriesToInclude) {
            val catChannels = rawList.filter { it.category == cat }
            val fallbacks = richFallbacks[cat] ?: emptyList()
            val combined = (catChannels + fallbacks)
                .distinctBy { it.name.trim().lowercase() }
                .take(40)
            groupedList.addAll(combined)
        }

        groupedList
    }

    private fun fetchAndCategorizeStreamed(categoryKey: String, url: String): List<Channel> {
        val channels = mutableListOf<Channel>()

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.charStream()?.use { reader ->
                    val bufferedReader = BufferedReader(reader)
                    var line: String?
                    var currentName = ""
                    var currentLogo = ""

                    while (bufferedReader.readLine().also { line = it } != null) {
                        val trimmed = line!!.trim()
                        if (trimmed.startsWith("#EXTINF:")) {
                            currentLogo = extractAttribute(trimmed, "tvg-logo") ?: ""
                            val tvgName = extractAttribute(trimmed, "tvg-name")
                            val commaIndex = trimmed.lastIndexOf(',')
                            if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                                currentName = trimmed.substring(commaIndex + 1).trim()
                            } else if (!tvgName.isNullOrEmpty()) {
                                currentName = tvgName
                            }
                        } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            if (currentName.isEmpty()) {
                                currentName = "$categoryKey Channel ${channels.size + 1}"
                            }

                            val cleanName = currentName
                                .replace(Regex("\\[.*?\\]"), "")
                                .replace(Regex("\\(.*?\\)"), "")
                                .trim()
                                .ifEmpty { "$categoryKey Channel ${channels.size + 1}" }

                            val targetCategory = determineCategory(categoryKey, cleanName)

                            if (targetCategory == "Pakistan") {
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
                                    id = "${targetCategory.take(3)}_${System.currentTimeMillis()}_${channels.size}",
                                    name = cleanName,
                                    streamUrl = trimmed,
                                    logoUrl = currentLogo,
                                    category = targetCategory,
                                    country = targetCategory,
                                    contentType = if (targetCategory.contains("Movie")) "MOVIE" else if (targetCategory.contains("Series")) "SERIES" else "LIVE"
                                )
                            )
                            currentName = ""
                            currentLogo = ""

                            if (channels.size >= 120) break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching $categoryKey: ${e.message}")
        }

        return channels
    }

    private fun determineCategory(categoryKey: String, name: String): String {
        val lower = name.lowercase()
        return when (categoryKey) {
            "Movies Base" -> {
                when {
                    lower.contains("telugu") || lower.contains("tamil") || lower.contains("south") || lower.contains("tollywood") -> "Tollywood Movies"
                    lower.contains("hindi") || lower.contains("bollywood") || lower.contains("india") || lower.contains("cinema") -> "Bollywood Movies"
                    else -> "Hollywood Movies"
                }
            }
            "Series Base" -> {
                when {
                    lower.contains("hindi") || lower.contains("bollywood") || lower.contains("india") -> "Bollywood Web Series"
                    else -> "Hollywood Web Series"
                }
            }
            else -> categoryKey
        }
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"([^\"]*)\""
        val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(line)
        return match?.groups?.get(1)?.value
    }
}
