package com.example.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.Channel
import com.example.ui.theme.RedAccent

@OptIn(UnstableApi::class)
@Composable
fun MixedVideoPlayerView(
    channel: Channel,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = true
) {
    val context = LocalContext.current
    var isOffline by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

    val exoPlayer = remember(channel) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://www.google.com/",
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
            )
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        // Optimized load control to eliminate video stutter and freezing
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBufferMs
                50000, // maxBufferMs
                2500,  // bufferForPlaybackMs
                5000   // bufferForPlaybackAfterRebufferMs
            )
            .build()

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(channel.streamUrl)
                setMediaItem(mediaItem)
                this.playWhenReady = playWhenReady
                prepare()
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        isOffline = false
                    }
                    Player.STATE_ENDED -> isBuffering = false
                    Player.STATE_IDLE -> {}
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                isOffline = true
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && !isOffline) {
            CircularProgressIndicator(color = RedAccent)
        }

        if (isOffline) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Stream Offline - Tap to Retry",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isOffline = false
                            isBuffering = true
                            try {
                                exoPlayer.stop()
                                exoPlayer.clearMediaItems()
                                exoPlayer.setMediaItem(MediaItem.fromUri(channel.streamUrl))
                                exoPlayer.prepare()
                                exoPlayer.play()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                    ) {
                        Text(text = "Tap to Retry", color = Color.White)
                    }
                }
            }
        }
    }
}
