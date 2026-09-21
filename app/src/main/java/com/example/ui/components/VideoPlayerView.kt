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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.data.Channel
import com.example.ui.theme.RedAccent

@Composable
fun MixedVideoPlayerView(
    channel: Channel,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = true
) {
    HlsExoPlayerView(
        channel = channel,
        modifier = modifier,
        playWhenReady = playWhenReady
    )
}

@OptIn(UnstableApi::class)
@Composable
fun HlsExoPlayerView(
    channel: Channel,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = true
) {
    val context = LocalContext.current
    var isOffline by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var usingBackup by remember { mutableStateOf(false) }

    val exoPlayer = remember {
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

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        isOffline = false
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                    }
                    Player.STATE_IDLE -> {}
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                if (!usingBackup && channel.backupStreamUrl.isNotEmpty()) {
                    // Automatically fallback to backup stream without showing offline yet
                    usingBackup = true
                    isBuffering = true
                    try {
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        exoPlayer.setMediaItem(MediaItem.fromUri(channel.backupStreamUrl))
                        exoPlayer.prepare()
                        exoPlayer.play()
                    } catch (e: Exception) {
                        isOffline = true
                        isBuffering = false
                    }
                } else {
                    isOffline = true
                    isBuffering = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(channel) {
        usingBackup = false
        isBuffering = true
        isOffline = false
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        
        val targetUrl = channel.streamUrl.ifEmpty { channel.backupStreamUrl }
        val mediaItem = MediaItem.fromUri(targetUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.playWhenReady = playWhenReady
        exoPlayer.prepare()

        // Wait up to 10 seconds for playback ready; if it doesn't ready up and backup wasn't tried, try backup
        kotlinx.coroutines.delay(10000)
        if (exoPlayer.playbackState != Player.STATE_READY && exoPlayer.playbackState != Player.STATE_ENDED) {
            if (!usingBackup && channel.backupStreamUrl.isNotEmpty()) {
                usingBackup = true
                try {
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    exoPlayer.setMediaItem(MediaItem.fromUri(channel.backupStreamUrl))
                    exoPlayer.prepare()
                    exoPlayer.play()
                } catch (e: Exception) {
                    isOffline = true
                    isBuffering = false
                }
            } else if (exoPlayer.playbackState != Player.STATE_READY) {
                isOffline = true
                isBuffering = false
            }
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

        if (isBuffering) {
            CircularProgressIndicator(
                color = RedAccent
            )
        }

        if (isOffline) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = "Server Offline / Stream Unavailable",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            isOffline = false
                            isBuffering = true
                            usingBackup = false
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
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RedAccent)
                    ) {
                        androidx.compose.material3.Text(text = "Retry Stream", color = Color.White)
                    }
                }
            }
        }
    }
}
