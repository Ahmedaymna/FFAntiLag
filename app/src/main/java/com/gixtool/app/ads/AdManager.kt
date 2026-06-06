package com.gixtool.app.ads

import android.app.Activity
import android.util.Log
import android.widget.FrameLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val activity: Activity) {

    companion object {
        const val BANNER_ID = "ca-app-pub-6718038985057828/5364644957"
        const val INTERSTITIAL_ID = "ca-app-pub-6718038985057828/4460277306"
        const val REWARDED_ID = "ca-app-pub-6718038985057828/7252441697"
        private const val TAG = "AdManager"
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var bannerAdView: AdView? = null

    // ─── Banner Ad ────────────────────────────────────────────────────────────
    fun loadBannerAd(container: FrameLayout) {
        bannerAdView = AdView(activity).apply {
            adUnitId = BANNER_ID
            setAdSize(AdSize.BANNER)
            adListener = object : AdListener() {
                override fun onAdLoaded() { Log.d(TAG, "Banner loaded") }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Banner failed: ${error.message}")
                }
            }
        }
        container.removeAllViews()
        container.addView(bannerAdView)
        bannerAdView?.loadAd(AdRequest.Builder().build())
    }

    // ─── Interstitial Ad ─────────────────────────────────────────────────────
    fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, INTERSTITIAL_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed: ${error.message}")
                }
            })
    }

    fun showInterstitialAd(onDismissed: () -> Unit = {}) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    onDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    onDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onDismissed()
            loadInterstitialAd()
        }
    }

    // ─── Rewarded Ad ─────────────────────────────────────────────────────────
    fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, REWARDED_ID, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded failed: ${error.message}")
                }
            })
    }

    fun showRewardedAd(onRewarded: (type: String, amount: Int) -> Unit, onDismissed: () -> Unit = {}) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd()
                    onDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    onDismissed()
                }
            }
            rewardedAd?.show(activity) { rewardItem ->
                onRewarded(rewardItem.type, rewardItem.amount)
            }
        } else {
            onDismissed()
            loadRewardedAd()
        }
    }

    fun destroy() {
        bannerAdView?.destroy()
    }
}
