package com.ncmine.importmine.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TAG = "NC_ADS"

// IDs Reais fornecidos pelo usuário
private const val APP_ID = "ca-app-pub-8967995144964134~3943247492"
private const val BANNER_AD_UNIT_ID = "ca-app-pub-8967995144964134/3519728209"
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-8967995144964134/4648977046"
private const val NATIVE_AD_UNIT_ID = "ca-app-pub-8967995144964134/7267401520"

object AdMobManager {

    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false
    private var isLoadingRewarded = false

    /**
     * Inicializa o SDK e configura os logs
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        Log.d(TAG, "Iniciando inicialização do AdMob...")
        
        MobileAds.initialize(context) { status ->
            isInitialized = true
            Log.d(TAG, "AdMob inicializado com sucesso!")
            
            // Configura para não ser modo de teste
            val configuration = RequestConfiguration.Builder().build()
            MobileAds.setRequestConfiguration(configuration)
            
            loadRewardedAd(context)
        }
    }

    /**
     * Carrega o anúncio de vídeo com logs detalhados
     */
    fun loadRewardedAd(context: Context) {
        if (isLoadingRewarded || rewardedAd != null) {
            Log.d(TAG, "Rewarded ad já está carregado ou carregando...")
            return
        }

        isLoadingRewarded = true
        Log.d(TAG, "Solicitando carregamento de Rewarded Ad: $REWARDED_AD_UNIT_ID")
        
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    Log.d(TAG, "SUCESSO: Vídeo Premiado pronto para uso!")

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            Log.d(TAG, "Vídeo fechado pelo usuário. Carregando próximo...")
                            loadRewardedAd(context)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            rewardedAd = null
                            Log.e(TAG, "ERRO AO MOSTRAR VÍDEO: ${error.message}")
                            loadRewardedAd(context)
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    Log.e(TAG, "FALHA AO CARREGAR VÍDEO (Erro ${error.code}): ${error.message}")
                    
                    // RETRY LOGIC: Tenta carregar novamente após 15 segundos se falhar
                    // Isso ajuda quando o erro é temporário ou falta de estoque
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "Tentando carregar anúncio novamente após falha...")
                        loadRewardedAd(context)
                    }, 15000)
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        if (rewardedAd != null) {
            Log.d(TAG, "Mostrando vídeo premiado...")
            rewardedAd?.show(activity) {
                Log.d(TAG, "Usuário completou o vídeo! Liberando recompensa.")
                onRewardEarned()
            }
        } else {
            Log.w(TAG, "Vídeo não disponível. Liberando acesso para não travar o usuário.")
            loadRewardedAd(activity)
            Toast.makeText(activity, "Vídeo carregando... Acesso liberado!", Toast.LENGTH_SHORT).show()
            onRewardEarned()
        }
    }

    fun getBannerAdUnitId(): String = BANNER_AD_UNIT_ID
    fun getNativeAdUnitId(): String = NATIVE_AD_UNIT_ID
    fun createAdRequest(): AdRequest = AdRequest.Builder().build()
}
