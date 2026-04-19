package com.ncmine.importmine.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


private const val TAG = "NC_ADS"

// IDs Reais fornecidos pelo usuário
private const val APP_ID = "ca-app-pub-8967995144964134~3943247492"
private const val BANNER_AD_UNIT_ID = "ca-app-pub-8967995144964134/3519728209"
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-8967995144964134/4648977046"
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-8967995144964134/7267401520" // Usando o ID que estava como Native para Interstitial se necessário, ou solicitar novo

@Singleton
class AdMobManager @Inject constructor(@ApplicationContext private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingRewarded = false
    private var isLoadingInterstitial = false
    
    // Estado Premium persistido
    var isPremium: Boolean
        get() = context.getSharedPreferences("ncmine_prefs", Context.MODE_PRIVATE).getBoolean("is_premium", false)
        set(value) = context.getSharedPreferences("ncmine_prefs", Context.MODE_PRIVATE).edit().putBoolean("is_premium", value).apply()



    /**
     * Carrega o anúncio de vídeo premiado
     */
    fun loadRewardedAd(context: Context) {
        if (isPremium || isLoadingRewarded || rewardedAd != null) return

        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    Log.d(TAG, "Rewarded Ad carregado.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    Log.e(TAG, "Falha ao carregar Rewarded Ad: ${error.message}")
                }
            }
        )
    }

    /**
     * Carrega o anúncio Interstitial
     */
    fun loadInterstitialAd(context: Context) {
        if (isPremium || isLoadingInterstitial || interstitialAd != null) return

        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "Interstitial Ad carregado.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    Log.e(TAG, "Falha ao carregar Interstitial Ad: ${error.message}")
                }
            }
        )
    }

    /**
     * Mostra o anúncio Interstitial em momentos estratégicos (ex: após scan)
     */
    fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        if (isPremium || interstitialAd == null) {
            onAdClosed()
            return
        }

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd(activity)
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onAdClosed()
            }
        }
        interstitialAd?.show(activity)
    }

    /**
     * Mostra o anúncio Premiado para desbloquear recursos
     */
    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        if (isPremium) {
            onRewardEarned()
            return
        }

        if (rewardedAd != null) {
            rewardedAd?.show(activity) {
                onRewardEarned()
            }
        } else {
            Toast.makeText(activity, "Anúncio carregando... Tente novamente em instantes.", Toast.LENGTH_SHORT).show()
            loadRewardedAd(activity)
        }
    }

    fun getBannerAdUnitId(): String = BANNER_AD_UNIT_ID

    fun createAdRequest(): AdRequest = AdRequest.Builder().build()
}
