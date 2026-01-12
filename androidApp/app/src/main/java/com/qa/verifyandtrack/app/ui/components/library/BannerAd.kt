package com.qa.verifyandtrack.app.ui.components.library

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.qa.verifyandtrack.app.BuildConfig
import com.qa.verifyandtrack.app.data.service.AdService

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adService = remember { AdService(context) }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                adSize = AdSize.BANNER
                adUnitId = BuildConfig.ADMOB_BANNER_ID

                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("BannerAd", "Ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(loadError: LoadAdError) {
                        Log.e("BannerAd", "Ad failed to load: ${loadError.message}")
                    }

                    override fun onAdOpened() {
                        Log.d("BannerAd", "Ad opened")
                    }

                    override fun onAdClicked() {
                        Log.d("BannerAd", "Ad clicked")
                    }

                    override fun onAdClosed() {
                        Log.d("BannerAd", "Ad closed")
                    }
                }

                loadAd(adService.createBannerAdRequest())
            }
        },
        update = { adView ->
            // Ad refreshes automatically
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            // AdView cleanup is handled automatically
        }
    }
}
