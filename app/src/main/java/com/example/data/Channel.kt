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
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Famous / Priority Channels with Original Logos placed at the VERY FRONT of each category
    val priorityChannels = listOf(
        // Pakistan Famous Channels
        Channel("fam_pk_1", "Geo News", "https://live.geo.tv/georaw/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/e/e4/Geo_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("fam_pk_2", "ARY News", "https://live-arynews.live-stream.com.pk/arynews/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/3/36/ARY_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("fam_pk_3", "Hum News", "https://live-humnews.live-stream.com.pk/humnews/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/d/d3/Hum_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("fam_pk_4", "PTV Sports", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Sports", "Pakistan", "LIVE"),
        Channel("fam_pk_5", "Geo Super", "https://live.geosuper.tv/geosuper/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/4/41/Geo_Super_logo.png", "Sports", "Pakistan", "LIVE"),
        Channel("fam_pk_6", "Express News", "https://live.expressnews.tv/express/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/8/8e/Express_News_Logo.png", "News", "Pakistan", "LIVE"),
        Channel("fam_pk_7", "Dunya News", "https://live.dunyanews.tv/dunya/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/f/fa/Dunya_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("fam_pk_8", "Samaa TV", "https://live.samaa.tv/samaa/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/1/1d/Samaa_TV_logo.png", "News", "Pakistan", "LIVE"),

        // India Famous Channels
        Channel("fam_in_1", "Aaj Tak", "https://vidgyor.com/aajtak/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Aaj_Tak_logo.png", "News", "India", "LIVE"),
        Channel("fam_in_2", "NDTV 24x7", "https://ndtv24x7.live-s.cdn.bitgravity.com/cdn/ndtv24x7/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/a/ac/NDTV_24x7_logo.png", "News", "India", "LIVE"),
        Channel("fam_in_3", "Zee News", "https://zee-news.live-s.cdn.bitgravity.com/cdn/zeenews/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/6/6d/Zee_News_logo.png", "News", "India", "LIVE"),
        Channel("fam_in_4", "ABP News", "https://abp-news.live-s.cdn.bitgravity.com/cdn/abpnews/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/4/47/ABP_News_logo.png", "News", "India", "LIVE"),

        // Turkey Famous Channels
        Channel("fam_tr_1", "TRT World", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/7/7b/TRT_World_logo.png", "News", "Turkey", "LIVE"),
        Channel("fam_tr_2", "TRT Haber", "https://tv-trthaber.trt.com.tr/master.m3u8", "https://upload.wikimedia.org/wikipedia/commons/b/b3/TRT_Haber_logo.png", "News", "Turkey", "LIVE"),
        Channel("fam_tr_3", "TRT Spor", "https://tv-trtspor.trt.com.tr/master.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/22/TRT_Spor_logo.png", "Sports", "Turkey", "LIVE"),

        // Chinese & Korean Hindi Dubbed
        Channel("dub_1", "Chinese Action Movie (Hindi Dubbed)", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300", "Chinese Hindi Dubbed", "China", "MOVIE"),
        Channel("dub_2", "Chinese Fantasy Drama (Hindi Dubbed)", "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8", "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=300", "Chinese Hindi Dubbed", "China", "SERIES"),
        Channel("dub_3", "Korean Romantic Drama (Hindi Dubbed)", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300", "Korean Hindi Dubbed", "Korea", "SERIES"),
        Channel("dub_4", "Korean Thriller Series (Hindi Dubbed)", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=300", "Korean Hindi Dubbed", "Korea", "SERIES"),

        // USA / International
        Channel("intl_1", "Bloomberg TV International", "https://live.bloomberg.com/android/master.m3u8", "https://upload.wikimedia.org/wikipedia/commons/d/ed/Bloomberg_Television_logo.svg", "USA / International", "USA", "LIVE"),
        Channel("intl_2", "NASA TV HD", "https://nasa-i.akamaihd.net/hls/live/509607/NASA-TVHD/master.m3u8", "https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg", "USA / International", "USA", "LIVE")
    )

    val sourceUrls = mapOf(
        "Pakistan" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/pk.m3u",
        "India" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in.m3u",
        "Turkey" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/tr.m3u",
        "USA / International" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/us.m3u",
        "Cartoons" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/animation.m3u",
        "News" to "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/news.m3u"
    )

    suspend fun fetchAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val parsedChannels = mutableListOf<Channel>()
        // Add famous priority channels first so they appear at the very top of categories
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
                            m3uUrl.contains("in.m3u") -> "India"
                            m3uUrl.contains("tr.m3u") -> "Turkey"
                            m3uUrl.contains("us.m3u") -> "USA / International"
                            m3uUrl.contains("animation.m3u") -> "Cartoons"
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
}

object SampleData {
    val channels = ChannelRepository.priorityChannels
    val musicChannels = ChannelRepository.priorityChannels
}
