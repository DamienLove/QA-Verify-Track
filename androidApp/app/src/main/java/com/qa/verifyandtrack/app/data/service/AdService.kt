package com.qa.verifyandtrack.app.data.service

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.qa.verifyandtrack.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdService(private val context: Context) {

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var lastInterstitialShowTime: Long = 0
    private val minInterstitialInterval = 5 * 60 * 1000L // 5 minutes

    companion object {
        private const val TAG = "AdService"
    }

    /**
     * Initialize AdMob SDK
     * Should be called once when app starts
     */
    fun initialize() {
        if (_isInitialized.value) {
            Log.d(TAG, "AdMob already initialized")
            return
        }

        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob initialized: ${status.adapterStatusMap}")
                _isInitialized.value = true
                // Preload first interstitial ad
                loadInterstitialAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdMob", e)
        }
    }

    /**
     * Load interstitial ad in background
     */
    fun loadInterstitialAd() {
        if (isLoadingInterstitial || interstitialAd != null) {
            Log.d(TAG, "Already loading or ad loaded")
            return
        }

        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isLoadingInterstitial = false

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad dismissed")
                            interstitialAd = null
                            lastInterstitialShowTime = System.currentTimeMillis()
                            // Load next ad
                            loadInterstitialAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                            interstitialAd = null
                            isLoadingInterstitial = false
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad showed")
                        }
                    }
                }

                override fun onAdFailedToLoad(loadError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${loadError.message}")
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    /**
     * Show interstitial ad if available and frequency cap allows
     * @param onAdDismissed Callback when ad is dismissed or not shown
     * @param forceShow Bypass frequency cap (for specific events)
     */
    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit = {},
        forceShow: Boolean = false
    ) {
        // Check frequency cap
        if (!forceShow) {
            val timeSinceLastAd = System.currentTimeMillis() - lastInterstitialShowTime
            if (timeSinceLastAd < minInterstitialInterval) {
                Log.d(TAG, "Frequency cap: Too soon to show ad (${timeSinceLastAd / 1000}s ago)")
                onAdDismissed()
                return
            }
        }

        // Show ad if loaded
        if (interstitialAd != null) {
            Log.d(TAG, "Showing interstitial ad")
            interstitialAd?.show(activity)
            // onAdDismissed will be called by fullScreenContentCallback
        } else {
            Log.d(TAG, "Interstitial ad not ready, loading...")
            loadInterstitialAd()
            onAdDismissed()
        }
    }

    /**
     * Check if enough time has passed since last interstitial
     */
    fun canShowInterstitial(): Boolean {
        val timeSinceLastAd = System.currentTimeMillis() - lastInterstitialShowTime
        return timeSinceLastAd >= minInterstitialInterval
    }

    /**
     * Check if interstitial ad is loaded and ready
     */
    fun isInterstitialReady(): Boolean {
        return interstitialAd != null
    }

    /**
     * Create AdRequest for banner ads
     */
    fun createBannerAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }
}
