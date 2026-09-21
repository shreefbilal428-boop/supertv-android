package com.example.data

data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String = "",
    val streamUrl: String,
    val country: String,
    val category: String,
    val isLive: Boolean = true
)

object SampleData {
    val channels = listOf(
        Channel(
            id = "sample_hls_1",
            name = "Big Buck Bunny HLS Live",
            logoUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=300",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            country = "Pakistan",
            category = "Entertainment"
        ),
        Channel(
            id = "sample_hls_2",
            name = "Sintel HD Stream",
            logoUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300",
            streamUrl = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            country = "Bollywood",
            category = "Movies"
        )
    )

    val musicChannels = listOf(
        Channel(
            id = "music_hls_1",
            name = "Sample Audio Stream",
            logoUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            country = "Music",
            category = "Hits"
        )
    )
}
