package com.restlock.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.fitness.restlock.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CreatorSupportRewardedAd {
    private const val TAG = "CreatorSupportAd"

    @Volatile
    private var rewardedAd: RewardedAd? = null

    @Volatile
    private var loading = false

    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context) {}
            preload(context.applicationContext)
        }
    }

    fun preload(context: Context) {
        if (loading || rewardedAd != null) return
        loading = true
        RewardedAd.load(
            context,
            context.getString(R.string.admob_rewarded_finish_workout_ad_unit_id),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded.")
                    rewardedAd = ad
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                    loading = false
                }
            },
        )
    }

    fun showOrContinue(
        activity: Activity,
        onContinue: () -> Unit,
    ) {
        val cachedAd = rewardedAd
        if (cachedAd != null) {
            showLoadedAd(activity, cachedAd, onContinue)
            return
        }

        if (loading) {
            onContinue()
            return
        }

        loading = true
        RewardedAd.load(
            activity,
            activity.getString(R.string.admob_rewarded_finish_workout_ad_unit_id),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loading = false
                    showLoadedAd(activity, ad, onContinue)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Rewarded ad failed to load on demand: ${error.message}")
                    loading = false
                    rewardedAd = null
                    onContinue()
                }
            },
        )
    }

    private fun showLoadedAd(
        activity: Activity,
        ad: RewardedAd,
        onContinue: () -> Unit,
    ) {
        var completed = false

        fun completeOnce() {
            if (completed) return
            completed = true
            rewardedAd = null
            preload(activity.applicationContext)
            onContinue()
        }

        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                completeOnce()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "Rewarded ad failed to show: ${adError.message}")
                completeOnce()
            }
        }

        ad.show(activity) {
            Log.d(TAG, "User earned creator support reward.")
        }
    }
}
