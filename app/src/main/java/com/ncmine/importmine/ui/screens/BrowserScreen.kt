package com.ncmine.importmine.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ncmine.importmine.ui.theme.NcBlackDeep
import com.ncmine.importmine.ui.theme.NcGreenNeon

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    url: String,
    onBack: () -> Unit,
    onFileDownloaded: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var webView: WebView? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Baixar Add-on", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recarregar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NcBlackDeep,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = NcGreenNeon
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(NcBlackDeep)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.databaseEnabled = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.allowContentAccess = true
                        settings.allowFileAccess = true
                        
                        // User agent mais moderno para evitar bloqueios de bots
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                
                                // Se for um link externo que o WebView não consegue lidar bem (deep links)
                                if (url.startsWith("intent://") || url.contains("play.google.com") || url.startsWith("market://")) {
                                    try {
                                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                        context.startActivity(intent)
                                        return true
                                    } catch (e: Exception) {
                                        // Fallback se falhar
                                    }
                                }
                                
                                // Links que devem abrir no navegador externo (opcional, dependendo do comportamento desejado)
                                // if (url.contains("terabox.com") && !url.contains("m.terabox.com")) { ... }

                                return false // Deixa o WebView lidar com a navegação normal
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            // Quando o download começa, avisamos o app para ficar de olho
                            // No Android, o DownloadManager cuida disso.
                            // Mas para o nosso fluxo "automático", queremos voltar e escanear.
                            onFileDownloaded()
                        }

                        loadUrl(url)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = NcGreenNeon,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
