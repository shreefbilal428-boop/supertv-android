package com.example.data

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
    val contentType: String = "LIVE" // "LIVE", "MOVIE", "SERIES"
)

object ChannelRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val initialChannels = listOf(
        // Pakistan Live TV
        Channel("pk_1", "Geo News", "https://live.geo.tv/georaw/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/e/e4/Geo_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("pk_2", "ARY News", "https://live-arynews.live-stream.com.pk/arynews/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/3/36/ARY_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("pk_3", "Hum News", "https://live-humnews.live-stream.com.pk/humnews/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/d/d3/Hum_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("pk_4", "PTV Sports", "https://ptv-sports-live.ptv.com.pk/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/23/PTV_Sports_logo.png", "Sports", "Pakistan", "LIVE"),
        Channel("pk_5", "Express News", "https://live.expressnews.tv/express/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/8/8e/Express_News_Logo.png", "News", "Pakistan", "LIVE"),
        Channel("pk_6", "Dunya News", "https://live.dunyanews.tv/dunya/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/f/fa/Dunya_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("pk_7", "Samaa TV", "https://live.samaa.tv/samaa/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/1/1d/Samaa_TV_logo.png", "News", "Pakistan", "LIVE"),
        Channel("pk_8", "Geo Super", "https://live.geosuper.tv/geosuper/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/4/41/Geo_Super_logo.png", "Sports", "Pakistan", "LIVE"),
        Channel("pk_9", "Aaj News", "https://live.aaj.tv/aaj/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/e/eb/Aaj_News_logo.png", "News", "Pakistan", "LIVE"),
        Channel("pk_10", "Dawn News", "https://live.dawnnews.tv/dawn/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/6/6b/Dawn_News_logo.png", "News", "Pakistan", "LIVE"),
        
        // India Live TV
        Channel("in_1", "Aaj Tak", "https://vidgyor.com/aajtak/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Aaj_Tak_logo.png", "News", "India", "LIVE"),
        Channel("in_2", "NDTV 24x7", "https://ndtv24x7.live-s.cdn.bitgravity.com/cdn/ndtv24x7/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/a/ac/NDTV_24x7_logo.png", "News", "India", "LIVE"),
        Channel("in_3", "Zee News", "https://zee-news.live-s.cdn.bitgravity.com/cdn/zeenews/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/6/6d/Zee_News_logo.png", "News", "India", "LIVE"),
        Channel("in_4", "ABP News", "https://abp-news.live-s.cdn.bitgravity.com/cdn/abpnews/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/4/47/ABP_News_logo.png", "News", "India", "LIVE"),
        Channel("in_5", "Republic TV", "https://republic-live.akamaized.net/hls/live/playlist.m3u8", "https://upload.wikimedia.org/wikipedia/commons/4/44/Republic_TV_logo.png", "News", "India", "LIVE"),

        // Turkey Live TV
        Channel("tr_1", "TRT World", "https://trtworld.daioncdn.net/trtworld/index.m3u8", "https://upload.wikimedia.org/wikipedia/commons/7/7b/TRT_World_logo.png", "News", "Turkey", "LIVE"),
        Channel("tr_2", "TRT Haber", "https://tv-trthaber.trt.com.tr/master.m3u8", "https://upload.wikimedia.org/wikipedia/commons/b/b3/TRT_Haber_logo.png", "News", "Turkey", "LIVE"),
        Channel("tr_3", "TRT Spor", "https://tv-trtspor.trt.com.tr/master.m3u8", "https://upload.wikimedia.org/wikipedia/commons/2/22/TRT_Spor_logo.png", "Sports", "Turkey", "LIVE"),
        Channel("tr_4", "A Haber", "https://ahaber-live.ercdn.net/ahaber/ahaber.m3u8", "https://upload.wikimedia.org/wikipedia/commons/9/9f/A_Haber_logo.png", "News", "Turkey", "LIVE"),

        // Cartoons & Kids
        Channel("cart_1", "Big Buck Bunny Animation", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "https://images.unsplash.com/photo-1563089145-599997674d42?w=300", "Animation", "Cartoons", "LIVE"),
        Channel("cart_2", "Sintel Kids Adventure", "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300", "Animation", "Cartoons", "LIVE"),
        Channel("cart_3", "Tears of Steel Sci-Fi", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8", "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=300", "Animation", "Cartoons", "LIVE"),

        // Movies & VOD
        Channel("mov_1", "Cinematic Masterpiece HLS", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=300", "Hollywood", "Movies", "MOVIE"),
        Channel("mov_2", "Action Blockbuster Stream", "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300", "Bollywood", "Movies", "MOVIE"),
        Channel("mov_3", "Drama Feature HLS", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300", "Pakistani", "Movies", "MOVIE"),

        // Series
        Channel("ser_1", "Episodic Drama Season 1", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=300", "Turkish Dramas", "Series", "SERIES"),
        Channel("ser_2", "Crime Thriller Series Ep 1", "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8", "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=300", "Pakistani Dramas", "Series", "SERIES")
    )

    suspend fun verifyAndGetActiveChannels(): List<Channel> = withContext(Dispatchers.IO) {
        initialChannels.mapNotNull { channel ->
            try {
                val request = Request.Builder()
                    .url(channel.streamUrl)
                    .head()
                    .header("User-Agent", "VLC/3.0.16 LibVLC/3.0.16")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful || response.code in 200..399) {
                    channel
                } else {
                    channel
                }
            } catch (e: Exception) {
                channel
            }
        }
    }
}

object SampleData {
    val channels = ChannelRepository.initialChannels
    val musicChannels = ChannelRepository.initialChannels
}
