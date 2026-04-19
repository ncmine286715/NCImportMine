package com.ncmine.importmine.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TAG = "NC_ADS"

// IDs Reais fornecidos pelo usuário
private const val APP_ID = "ca-app-pub-8967995144964134~3943247492"
private const val BANNER_AD_UNIT_ID = "ca-app-pub-8967995144964134/3519728209"
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-8967995144964134/4648977046"
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-8967995144964134/7267401520" // Usando o ID que estava como Native para Interstitial se necessário, ou solicitar novo

object AdMobManager {

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false
    private var isLoadingRewarded = false
    private var isLoadingInterstitial = false
    
    // Estado Premium (deve ser persistido via DataStore/SharedPreferences)
    var isPremium: Boolean = false

    /**
     * Inicializa o SDK e configura os logs
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        Log.d(TAG, "Iniciando inicialização do AdMob...")
        
        MobileAds.initialize(context) { status ->
            isInitialized = true
            Log.d(TAG, "AdMob inicializado com sucesso!")
            
            val configuration = RequestConfiguration.Builder().build()
            MobileAds.setRequestConfiguration(configuration)
            
            if (!isPremium) {
                loadRewardedAd(context)
                loadInterstitialAd(context)
            }
        }
    }

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

    /**
     * Cria uma requisição básica de anúncio
     */
    fun createAdRequest(): AdRequest = AdRequest.Builder().build()
}
