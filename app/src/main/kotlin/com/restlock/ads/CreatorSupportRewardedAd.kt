package com.restlock.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.fitness.restlock.R

object CreatorSupportRewardedAd {
    private const val TAG = "CreatorSupportAd"
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var rewardedAd: RewardedAd? = null

    @Volatile
    private var loading = false

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        runOnMain {
            runCatching {
                MobileAds.initialize(appContext) {
                    preload(appContext)
                }
            }.onFailure {
                Log.d(TAG, "Mobile Ads initialization failed: ${it.message}")
                loading = false
            }
        }
    }

    fun preload(context: Context) {
        val appContext = context.applicationContext
        runOnMain {
            if (loading || rewardedAd != null) return@runOnMain
            loading = true
            runCatching {
                RewardedAd.load(
                    appContext,
                    appContext.getString(R.string.admob_rewarded_finish_workout_ad_unit_id),
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
            }.onFailure {
                Log.d(TAG, "Rewarded ad preload crashed: ${it.message}")
                rewardedAd = null
                loading = false
            }
        }
    }

    fun showOrContinue(
        activity: Activity,
        onContinue: () -> Unit,
    ) {
        runOnMain {
            showOrContinueOnMain(activity, onContinue)
        }
    }

    private fun showOrContinueOnMain(
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
        runCatching {
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
        }.onFailure {
            Log.d(TAG, "Rewarded ad load crashed: ${it.message}")
            loading = false
            rewardedAd = null
            onContinue()
        }
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

        runCatching {
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
        }.onFailure {
            Log.d(TAG, "Rewarded ad show crashed: ${it.message}")
            completeOnce()
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
