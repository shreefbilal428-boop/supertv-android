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
    val backupStreamUrl: String = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
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

    val priorityChannels = listOf(
        Channel(
            id = "pak_fam_1",
            name = "Hum TV",
            streamUrl = "https://live-humtv.live-stream.com.pk/humtv/index.m3u8",
            backupStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/d/d3/Hum_News_logo.png",
            category = "Pakistan",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_fam_2",
            name = "ARY Digital",
            streamUrl = "https://live-arydigital.live-stream.com.pk/arydigital/index.m3u8",
            backupStreamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/3/36/ARY_News_logo.png",
            category = "Pakistan",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_fam_3",
            name = "Geo Entertainment / Kahani",
            streamUrl = "https://live.geo.tv/georaw/index.m3u8",
            backupStreamUrl = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e4/Geo_News_logo.png",
            category = "Pakistan",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_fam_4",
            name = "Green TV Entertainment",
            streamUrl = "https://greentv-live.ercdn.net/greentv/greentv.m3u8",
            backupStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=300",
            category = "Pakistan",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_fam_5",
            name = "ARY Music",
            streamUrl = "https://live-arymusic.live-stream.com.pk/arymusic/index.m3u8",
            backupStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/3/36/ARY_News_logo.png",
            category = "Music",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_fam_6",
            name = "PTV Sports",
            streamUrl = "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8",
            backupStreamUrl = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png",
            category = "Sports",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_fam_7",
            name = "Geo Super",
            streamUrl = "https://live.geosuper.tv/geosuper/index.m3u8",
            backupStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/4/41/Geo_Super_logo.png",
            category = "Sports",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_fam_8",
            name = "One Plus / 8XM",
            streamUrl = "https://live.8xm.tv/8xm/index.m3u8",
            backupStreamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            logoUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
            category = "Music",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_fam_9",
            name = "Kids Cartoon 24/7",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            backupStreamUrl = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            logoUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=300",
            category = "Cartoons",
            country = "Cartoons",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_n_1",
            name = "Geo News",
            streamUrl = "https://live.geo.tv/georaw/index.m3u8",
            backupStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e4/Geo_News_logo.png",
            category = "News",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_n_2",
            name = "ARY News",
            streamUrl = "https://live-arynews.live-stream.com.pk/arynews/index.m3u8",
            backupStreamUrl = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/3/36/ARY_News_logo.png",
            category = "News",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_n_3",
            name = "Hum News",
            streamUrl = "https://live-humnews.live-stream.com.pk/humnews/index.m3u8",
            backupStreamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/d/d3/Hum_News_logo.png",
            category = "News",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_n_4",
            name = "Express News",
            streamUrl = "https://live.expressnews.tv/express/index.m3u8",
            backupStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/8/8e/Express_News_Logo.png",
            category = "News",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_n_5",
            name = "Dunya News",
            streamUrl = "https://live.dunyanews.tv/dunya/index.m3u8",
            backupStreamUrl = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/f/fa/Dunya_News_logo.png",
            category = "News",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "pak_n_6",
            name = "Samaa TV",
            streamUrl = "https://live.samaa.tv/samaa/index.m3u8",
            backupStreamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/1/1d/Samaa_TV_logo.png",
            category = "News",
            country = "Pakistan",
            contentType = "LIVE"
        ),
        Channel(
            id = "fam_in_1",
            name = "Aaj Tak",
            streamUrl = "https://vidgyor.com/aajtak/index.m3u8",
            backupStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/1/1a/Aaj_Tak_logo.png",
            category = "News",
            country = "India",
            contentType = "LIVE"
        ),
        Channel(
            id = "fam_tr_1",
            name = "TRT World",
            streamUrl = "https://trtworld.daioncdn.net/trtworld/index.m3u8",
            backupStreamUrl = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7b/TRT_World_logo.png",
            category = "News",
            country = "Turkey",
            contentType = "LIVE"
        ),
        Channel(
            id = "dub_1",
            name = "Chinese Action Movie (Hindi Dubbed)",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            backupStreamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            logoUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300",
            category = "Chinese Hindi Dubbed",
            country = "China",
            contentType = "MOVIE"
        ),
        Channel(
            id = "dub_3",
            name = "Korean Romantic Drama (Hindi Dubbed)",
            streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            backupStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300",
            category = "Korean Hindi Dubbed",
            country = "Korea",
            contentType = "SERIES"
        ),
        Channel(
            id = "intl_1",
            name = "Bloomberg TV International",
            streamUrl = "https://live.bloomberg.com/android/master.m3u8",
            backupStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/d/ed/Bloomberg_Television_logo.svg",
            category = "USA / International",
            country = "USA",
            contentType = "LIVE"
        )
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
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
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
                                backupStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
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
