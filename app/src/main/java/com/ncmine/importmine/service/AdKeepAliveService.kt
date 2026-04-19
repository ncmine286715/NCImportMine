package com.ncmine.importmine.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.ncmine.importmine.util.AdMobManager

/**
 * Serviço que mantém o processo vivo para pre-carregamento de anúncios
 * e garante que o app responda rapidamente ao retornar do background.
 */
class AdKeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val tag = "NC_ADS_SERVICE"

    private val stopRunnable = Runnable {
        Log.d(tag, "Tempo limite do serviço atingido. Finalizando.")
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "KeepAlive Service iniciado (60s)...")
        
        // Pre-carrega o anúncio premiado para estar pronto quando o usuário voltar
        AdMobManager.loadRewardedAd(this)

        handler.removeCallbacks(stopRunnable)
        handler.postDelayed(stopRunnable, 60000)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(tag, "Serviço destruído.")
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
