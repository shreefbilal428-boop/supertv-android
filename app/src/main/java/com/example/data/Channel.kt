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

    // Automatic public live IPTV playlist sources
    private val playlistUrls = listOf(
        "https://raw.githubusercontent.com/iptv-org/iptv/master/index.m3u",
        "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/pk.m3u"
    )

    suspend fun fetchAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        
        for (url in playlistUrls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: continue
                    val lines = body.lines()
                    var currentName = ""
                    var currentLogo = ""
                    var currentGroup = "General"

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
                            val channelId = "auto_${System.currentTimeMillis()}_${channels.size}"
                            channels.add(
                                Channel(
                                    id = channelId,
                                    name = currentName,
                                    streamUrl = trimmed,
                                    logoUrl = currentLogo,
                                    category = if (currentGroup.equals("Undefined", true) || currentGroup.isBlank()) "General" else currentGroup,
                                    country = currentGroup,
                                    contentType = "LIVE"
                                )
                            )
                            currentName = ""
                            currentLogo = ""
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChannelRepository", "Error fetching playlist $url: ${e.message}")
            }
        }

        // If automatic fetch returned nothing, provide fallback generic live streams
        if (channels.isEmpty()) {
            channels.add(
                Channel(
                    id = "fallback_1",
                    name = "Live Stream 1",
                    streamUrl = "https://trtworld.daioncdn.net/trtworld/index.m3u8",
                    logoUrl = "",
                    category = "General",
                    country = "International",
                    contentType = "LIVE"
                )
            )
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
    val channels = emptyList<Channel>()
    val musicChannels = emptyList<Channel>()
}
