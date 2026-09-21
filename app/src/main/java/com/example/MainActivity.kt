package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Channel
import com.example.ui.components.StartIoBannerAdContainer
import com.example.ui.components.StartIoInterstitialAdDialog
import com.example.ui.screens.FullscreenPlayerScreen
import com.example.ui.screens.LiveTvScreen
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.RedAccent
import com.example.ui.theme.SuperTvTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trustAllSslCertificates()
        enableEdgeToEdge()
        setContent {
            SuperTvTheme {
                SuperTvApp()
            }
        }
    }
}

private fun trustAllSslCertificates() {
    try {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        })
        val sc = SSLContext.getInstance("TLS")
        sc.init(null, trustAllCerts, java.security.SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

enum class NavigationTab(val title: String) {
    LIVE_TV("Live TV")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperTvApp() {
    var selectedTab by remember { mutableStateOf(NavigationTab.LIVE_TV) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var showInterstitialAd by remember { mutableStateOf(false) }
    var channelToPlay by remember { mutableStateOf<Channel?>(null) }

    // Interstitial Ad Dialog logic
    if (showInterstitialAd && channelToPlay != null) {
        StartIoInterstitialAdDialog(
            channelName = channelToPlay!!.name,
            onAdDismissed = {
                showInterstitialAd = false
                selectedChannel = channelToPlay
            }
        )
    }

    // Fullscreen Video Player Page
    if (selectedChannel != null) {
        FullscreenPlayerScreen(
            initialChannel = selectedChannel!!,
            onBackClick = { selectedChannel = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(RedAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "SUPER TV",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(RedAccent)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                ) {
                    // Persistent Container Reserved for Start.io Banner Ad underneath Navigation Bar
                    StartIoBannerAdContainer()
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LiveTvScreen(
                    onChannelSelected = { channel ->
                        channelToPlay = channel
                        showInterstitialAd = true
                    }
                )
            }
        }
    }
}

