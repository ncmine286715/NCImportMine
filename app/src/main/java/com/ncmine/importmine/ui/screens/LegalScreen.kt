package com.ncmine.importmine.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.ncmine.importmine.ui.theme.NcBlackDeep
import com.ncmine.importmine.ui.theme.NcGreenNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    url: String,
    title: String,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = NcGreenNeon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NcBlackDeep,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = NcBlackDeep
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasError) {
                ErrorView(onRetry = { hasError = false; isLoading = true })
            } else {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    hasError = true
                                    isLoading = false
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    
                                    // Trata links externos e esquemas especiais (como intent:// do Terabox/outros)
                                    if (url.startsWith("intent://") || url.contains("play.google.com") || url.startsWith("market://")) {
                                        try {
                                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                            context.startActivity(intent)
                                            return true
                                        } catch (e: Exception) {
                                            return false
                                        }
                                    }
                                    
                                    // Mantém a navegação dentro do WebView para links http/https normais
                                    return false
                                }
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            
                            // User Agent moderno para evitar que sites bloqueiem o WebView
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"

                            loadUrl(url)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { webView ->
                        // Only load if it's a different URL to prevent reload on recomposition
                        // though current logic should handle it via 'factory'
                    }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NcGreenNeon)
                }
            }
        }
    }
}

@Composable
fun ErrorView(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Erro de conexão", color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = NcGreenNeon)
        ) {
            Text("Tentar Novamente", color = Color.White)
        }
    }
}
