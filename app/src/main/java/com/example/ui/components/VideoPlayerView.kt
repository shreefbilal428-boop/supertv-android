package com.example.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DataSource
import androidx.media3.ui.PlayerView
import com.example.data.Channel
import com.example.ui.theme.RedAccent

@Composable
fun MixedVideoPlayerView(
    channel: Channel,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = true
) {
    var useWebViewFallback by remember(channel) { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (!useWebViewFallback) {
            HlsExoPlayerView(
                channel = channel,
                modifier = Modifier.fillMaxSize(),
                playWhenReady = playWhenReady,
                onFallbackToWebView = {
                    if (channel.embedUrl.isNotEmpty()) {
                        useWebViewFallback = true
                    }
                }
            )
        } else {
            WebViewPlayerView(
                embedUrl = channel.embedUrl.ifEmpty { channel.streamUrl },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (channel.embedUrl.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { useWebViewFallback = !useWebViewFallback },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (useWebViewFallback) "Switch to HLS Player" else "Switch to Web Stream",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun HlsExoPlayerView(
    channel: Channel,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = true,
    onFallbackToWebView: () -> Unit
) {
    val context = LocalContext.current
    var isOffline by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var usingBackup by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        val httpDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
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

        // Exact requested buffer settings
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBufferMs = 15000
                50000, // maxBufferMs = 50000
                2500,  // bufferForPlaybackMs = 2500
                5000   // bufferForPlaybackAfterRebufferMs = 5000
            )
            .build()

        val hlsMediaSourceFactory = HlsMediaSource.Factory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(hlsMediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
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
                if (!usingBackup && channel.backupStreamUrl.isNotEmpty()) {
                    usingBackup = true
                    isBuffering = true
                    try {
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        exoPlayer.setMediaItem(MediaItem.fromUri(channel.backupStreamUrl))
                        exoPlayer.prepare()
                        exoPlayer.play()
                    } catch (e: Exception) {
                        if (channel.embedUrl.isNotEmpty()) {
                            onFallbackToWebView()
                        } else {
                            isOffline = true
                            isBuffering = false
                        }
                    }
                } else {
                    if (channel.embedUrl.isNotEmpty()) {
                        onFallbackToWebView()
                    } else {
                        isOffline = true
                        isBuffering = false
                    }
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
        exoPlayer.setMediaItem(MediaItem.fromUri(targetUrl))
        exoPlayer.playWhenReady = playWhenReady
        exoPlayer.prepare()

        kotlinx.coroutines.delay(8000)
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
                    if (channel.embedUrl.isNotEmpty()) {
                        onFallbackToWebView()
                    } else {
                        isOffline = true
                        isBuffering = false
                    }
                }
            } else if (exoPlayer.playbackState != Player.STATE_READY) {
                if (channel.embedUrl.isNotEmpty()) {
                    onFallbackToWebView()
                } else {
                    isOffline = true
                    isBuffering = false
                }
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
            CircularProgressIndicator(color = RedAccent)
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
                    Text(
                        text = "Server Offline / Stream Unavailable",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (channel.embedUrl.isNotEmpty()) {
                                onFallbackToWebView()
                            } else {
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
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                    ) {
                        Text(text = if (channel.embedUrl.isNotEmpty()) "Open Web Stream" else "Retry Stream", color = Color.White)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewPlayerView(
    embedUrl: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = WebViewClient()
                loadUrl(embedUrl)
            }
        },
        modifier = modifier
    )
}
