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

    private val cartoonSources = listOf(
        "https://raw.githubusercontent.com/iptv-org/iptv/master/categories/kids.m3u",
        "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in.m3u",
        "https://raw.githubusercontent.com/iptv-org/iptv/master/categories/animation.m3u"
    )

    private val cartoonKeywords = listOf(
        "cartoon", "kids", "disney", "nick", "pogo", "hungama", "sonic", "toon", "anime", "junior", "baby", "boing", "pop", "cbeebies"
    )

    private val bollywoodSources = listOf(
        "https://iptv-org.github.io/iptv/countries/in.m3u",
        "https://iptv-org.github.io/iptv/languages/hin.m3u"
    )

    private val bollywoodKeywords = listOf(
        "star", "sony", "zee", "colors", "sab", "tv", "news", "music", "mtv", "bindass", "9xm", "mastiii", "b4u", "zoom", "aaj tak", "ndtv", "india"
    )

    private val richFallbacks = mapOf(
        "Pakistan" to listOf(
            Channel("pk_fb_1", "PTV Home", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_2", "PTV News", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_3", "Dunya News", "https://live.dunyanews.tv/dunya/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/f/fa/Dunya_News_logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_4", "Express News", "https://live.expressnews.tv/express/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/8/8e/Express_News_Logo.png", "Pakistan", "Pakistan", "LIVE"),
            Channel("pk_fb_5", "PTV Sports", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Pakistan", "Pakistan", "LIVE")
        ),
        "Bollywood" to listOf(
            Channel("in_fb_1", "Aaj Tak", "https://vidgyor.com/aajtak/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Aaj_Tak_logo.png", "Bollywood", "India", "LIVE"),
            Channel("in_fb_2", "NDTV 24x7", "https://ndtv24x7.live-s.cdn.bitgravity.com/cdn/ndtv24x7/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/a/ac/NDTV_24x7_logo.png", "Bollywood", "India", "LIVE")
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
        )
    )

    suspend fun fetchAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val standardMap = mapOf(
            "Pakistan" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/pk.m3u",
            "Turkey" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/tr.m3u",
            "Chinese" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/cn.m3u",
            "Bollywood Movies" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in.m3u",
            "Hollywood" to "https://raw.githubusercontent.com/iptv-org/iptv/master/categories/movies.m3u"
        )

        val deferredStandard = standardMap.map { (cat, url) ->
            async {
                fetchStandardCategory(cat, url)
            }
        }

        val deferredBollywood = async {
            fetchCombinedBollywood()
        }

        val deferredCartoons = async {
            fetchCombinedCartoons()
        }

        val standardResults = deferredStandard.awaitAll().flatten()
        val bollywoodResults = deferredBollywood.await()
        val cartoonsResults = deferredCartoons.await()

        standardResults + bollywoodResults + cartoonsResults
    }

    private fun fetchStandardCategory(category: String, url: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val allIndianChannels = mutableListOf<Channel>()
        val limit = 50

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
                                currentName = "$category Channel ${channels.size + 1}"
                            }

                            val cleanName = cleanChannelName(currentName, category)

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

                            val channelObj = Channel(
                                id = "${category.take(3)}_${System.currentTimeMillis()}_${channels.size + allIndianChannels.size}",
                                name = cleanName,
                                streamUrl = trimmed,
                                logoUrl = currentLogo,
                                category = category,
                                country = category,
                                contentType = if (category.contains("Movie") || category == "Hollywood") "MOVIE" else "LIVE"
                            )

                            if (category == "Bollywood Movies") {
                                allIndianChannels.add(channelObj)
                                val nameLower = cleanName.lowercase()
                                val keywords = listOf("cinema", "movie", "filam", "goldmines", "multiplex", "filmy", "action", "hitz", "star", "zee")
                                val matchesKeyword = keywords.any { nameLower.contains(it) }
                                if (matchesKeyword) {
                                    channels.add(channelObj)
                                }
                            } else {
                                channels.add(channelObj)
                            }

                            currentName = ""
                            currentLogo = ""

                            if (category != "Bollywood Movies" && channels.size >= limit) break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching $category: ${e.message}")
        }

        if (category == "Bollywood Movies" && channels.size < 20) {
            val additionalNeeded = limit - channels.size
            val fallbackFromIndian = allIndianChannels.filter { !channels.contains(it) }.take(additionalNeeded)
            channels.addAll(fallbackFromIndian)
        }

        val fallbacks = richFallbacks[category] ?: emptyList()
        val combined = (if (channels.isEmpty()) fallbacks else channels + fallbacks).take(limit)
        return combined.distinctBy { it.name.trim().lowercase() }
    }

    private fun fetchCombinedBollywood(): List<Channel> {
        val bollywoodChannels = mutableListOf<Channel>()

        for (url in bollywoodSources) {
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
                                    currentName = "Bollywood Channel ${bollywoodChannels.size + 1}"
                                }

                                val cleanName = cleanChannelName(currentName, "Bollywood")
                                val nameLower = cleanName.lowercase()

                                val matchesKeyword = bollywoodKeywords.any { nameLower.contains(it) } || url.contains("hin")

                                if (matchesKeyword) {
                                    bollywoodChannels.add(
                                        Channel(
                                            id = "bolly_${System.currentTimeMillis()}_${bollywoodChannels.size}",
                                            name = cleanName,
                                            streamUrl = trimmed,
                                            logoUrl = currentLogo,
                                            category = "Bollywood",
                                            country = "India",
                                            contentType = "LIVE"
                                        )
                                    )
                                }

                                currentName = ""
                                currentLogo = ""

                                if (bollywoodChannels.size >= 120) break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChannelRepository", "Error fetching bollywood from $url: ${e.message}")
            }
        }

        val fallbacks = richFallbacks["Bollywood"] ?: emptyList()
        val combined = (bollywoodChannels + fallbacks)
            .distinctBy { it.name.trim().lowercase() }
            .take(100)

        return combined
    }

    private fun fetchCombinedCartoons(): List<Channel> {
        val cartoonChannels = mutableListOf<Channel>()

        for (url in cartoonSources) {
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
                                    currentName = "Cartoon Channel ${cartoonChannels.size + 1}"
                                }

                                val cleanName = cleanChannelName(currentName, "Cartoons")
                                val nameLower = cleanName.lowercase()

                                val matchesKeyword = cartoonKeywords.any { nameLower.contains(it) } || url.contains("kids") || url.contains("animation")

                                if (matchesKeyword) {
                                    cartoonChannels.add(
                                        Channel(
                                            id = "cart_${System.currentTimeMillis()}_${cartoonChannels.size}",
                                            name = cleanName,
                                            streamUrl = trimmed,
                                            logoUrl = currentLogo,
                                            category = "Cartoons",
                                            country = "Global",
                                            contentType = "LIVE"
                                        )
                                    )
                                }

                                currentName = ""
                                currentLogo = ""

                                if (cartoonChannels.size >= 250) break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChannelRepository", "Error fetching cartoons from $url: ${e.message}")
            }
        }

        val fallbacks = richFallbacks["Cartoons"] ?: emptyList()
        val combined = (cartoonChannels + fallbacks)
            .distinctBy { it.name.trim().lowercase() }
            .take(200)

        return combined
    }

    private fun cleanChannelName(name: String, category: String): String {
        return name
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("(?i)720p"), "")
            .replace(Regex("(?i)1080p"), "")
            .replace(Regex("(?i)HD"), "")
            .replace(Regex("(?i)SD"), "")
            .trim()
            .ifEmpty { "$category Channel" }
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"([^\"]*)\""
        val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(line)
        return match?.groups?.get(1)?.value
    }
}
