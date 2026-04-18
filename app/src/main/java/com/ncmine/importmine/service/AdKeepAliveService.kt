package com.ncmine.importmine.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.ncmine.importmine.util.AdMobManager

/**
 * Serviço que mantém o processo vivo e processa anúncios em background por 1 minuto.
 */
class AdKeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "NC_ADS_SERVICE"
    private var backgroundAdView: AdView? = null

    private val stopRunnable = Runnable {
        Log.d(TAG, "Tempo esgotado. Finalizando anúncios de background.")
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Iniciando ciclo de anúncios invisíveis (60s)...")
        
        startBackgroundAds()

        handler.removeCallbacks(stopRunnable)
        handler.postDelayed(stopRunnable, 60000)

        return START_NOT_STICKY
    }

    private fun startBackgroundAds() {
        if (backgroundAdView == null) {
            backgroundAdView = AdView(this).apply {
                adUnitId = AdMobManager.getBannerAdUnitId()
                setAdSize(AdSize.BANNER)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "Anúncio invisível carregado com sucesso.")
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "Falha no anúncio invisível: ${error.message}")
                    }
                }
            }
        }
        
        // Carrega o primeiro anúncio
        backgroundAdView?.loadAd(AdMobManager.createAdRequest())
        
        // Opcional: Recarregar a cada 30 segundos dentro desse 1 minuto para maximizar
        handler.postDelayed({
            Log.d(TAG, "Recarregando anúncio invisível...")
            backgroundAdView?.loadAd(AdMobManager.createAdRequest())
        }, 30000)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Serviço destruído. Limpando recursos.")
        handler.removeCallbacksAndMessages(null)
        backgroundAdView?.destroy()
        backgroundAdView = null
        super.onDestroy()
    }
}
