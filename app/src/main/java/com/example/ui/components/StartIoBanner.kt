package com.example.ui.components

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.Ad

@Composable
fun StartIoBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            LinearLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                
                val bannerAd = Banner(context)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                bannerAd.layoutParams = params
                addView(bannerAd)
            }
        }
    )
}

fun showInterstitialAd(context: Context, onAdClosed: () -> Unit) {
    val startAppAd = StartAppAd(context)
    startAppAd.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
        override fun onReceiveAd(ad: Ad) {
            startAppAd.showAd(object : AdDisplayListener {
                override fun adHidden(ad: Ad) {
                    onAdClosed()
                }
                override fun adDisplayed(ad: Ad) {}
                override fun adClicked(ad: Ad) {}
                override fun adNotDisplayed(ad: Ad) {
                    onAdClosed()
                }
            })
        }

        override fun onFailedToReceiveAd(ad: Ad?) {
            onAdClosed()
        }
    })
}
