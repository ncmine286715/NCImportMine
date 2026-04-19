package com.ncmine.importmine.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.ncmine.importmine.ui.theme.NcBlackDeep
import com.ncmine.importmine.ui.theme.NcGreenNeon
import java.io.File
import java.net.URLDecoder

enum class TeraBoxStatus {
    LOADING,
    WAITING_LOGIN,
    READY_DOWNLOAD,
    DOWNLOADING,
    IDLE
}

class TeraBoxInterface(
    private val onStatusChange: (TeraBoxStatus) -> Unit,
    private val onDownloadStarted: () -> Unit
) {
    @JavascriptInterface
    fun updateStatus(status: String) {
        try {
            onStatusChange(TeraBoxStatus.valueOf(status))
        } catch (e: Exception) {}
    }

    @JavascriptInterface
    fun notifyDownloadStarted() {
        onDownloadStarted()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    url: String,
    onBack: () -> Unit,
    onDownloadCompleted: (File) -> Unit,
    onDownloadStarted: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(TeraBoxStatus.LOADING) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var showOverlay by remember { mutableStateOf(true) }

    val desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val jsAutomation = """
        (function() {
            // Fix viewport for better mobile viewing of desktop site
            if (!document.querySelector('meta[name="viewport"]')) {
                var meta = document.createElement('meta');
                meta.name = 'viewport';
                meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                document.getElementsByTagName('head')[0].appendChild(meta);
            }

            function findElementByText(texts, tags) {
                for (let tag of tags) {
                    let elements = document.getElementsByTagName(tag);
                    for (let el of elements) {
                        if (el.innerText) {
                            let content = el.innerText.toLowerCase();
                            if (texts.some(t => content.includes(t.toLowerCase()))) {
                                return el;
                            }
                        }
                    }
                }
                return null;
            }

            window.clickDownload = function() {
                let btn = findElementByText(["Download", "Baixar", "Gerar Link"], ["button", "a", "span", "div"]);
                if (btn) {
                    btn.click();
                }
            };

            function highlight(el, color) {
                if (!el) return;
                el.style.outline = '4px solid ' + color;
                el.style.outlineOffset = '2px';
                el.style.boxShadow = '0 0 20px ' + color;
            }

            let lastStatus = "";
            function checkPage() {
                let currentStatus = "IDLE";
                
                let loginBtn = findElementByText(["Google", "Entrar", "Sign in", "Login"], ["button", "a", "span", "div"]);
                let isLoginPage = document.body.innerText.toLowerCase().includes("login") || 
                                 document.body.innerText.toLowerCase().includes("entrar");

                let downloadBtn = findElementByText(["Download", "Baixar", "Gerar Link"], ["button", "a", "span", "div"]);

                if (downloadBtn) {
                    currentStatus = "READY_DOWNLOAD";
                    highlight(downloadBtn, "#3FB950");
                } else if (loginBtn && isLoginPage) {
                    currentStatus = "WAITING_LOGIN";
                    highlight(loginBtn, "#3FB950");
                }

                if (currentStatus !== lastStatus) {
                    lastStatus = currentStatus;
                    TeraBoxAndroid.updateStatus(currentStatus);
                }
            }

            setInterval(checkPage, 2000);
        })();
    """.trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TeraBox Auto-Import", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when(status) {
                                TeraBoxStatus.LOADING -> "Carregando site..."
                                TeraBoxStatus.WAITING_LOGIN -> "Ação necessária: Login"
                                TeraBoxStatus.READY_DOWNLOAD -> "Ação necessária: Download"
                                TeraBoxStatus.DOWNLOADING -> "Baixando arquivo..."
                                TeraBoxStatus.IDLE -> "Navegando..."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = NcGreenNeon
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showOverlay = !showOverlay }) {
                        Icon(
                            if (showOverlay) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Guia",
                            tint = if (showOverlay) NcGreenNeon else Color.White
                        )
                    }
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
        },
        floatingActionButton = {
            if (status == TeraBoxStatus.READY_DOWNLOAD) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        webView?.evaluateJavascript("window.clickDownload()", null)
                        Toast.makeText(context, "Solicitando download...", Toast.LENGTH_SHORT).show()
                    },
                    containerColor = NcGreenNeon,
                    contentColor = NcBlackDeep,
                    icon = { Icon(Icons.Default.Download, "Download") },
                    text = { Text("BAIXAR AGORA", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(NcBlackDeep)) {
            // AdMob Banner properly integrated at the top
            AdBannerPlaceholder()
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                cacheMode = WebSettings.LOAD_DEFAULT
                                databaseEnabled = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                allowContentAccess = true
                                allowFileAccess = true
                                javaScriptCanOpenWindowsAutomatically = true
                                userAgentString = desktopUA
                            }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            addJavascriptInterface(TeraBoxInterface(
                                onStatusChange = { status = it },
                                onDownloadStarted = { 
                                    status = TeraBoxStatus.DOWNLOADING
                                    onDownloadStarted()
                                }
                            ), "TeraBoxAndroid")

                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (status == TeraBoxStatus.LOADING) {
                                        status = TeraBoxStatus.IDLE
                                    }
                                    view?.evaluateJavascript(jsAutomation, null)
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val requestUrl = request?.url?.toString() ?: return false
                                    // Block external app redirects (intent://, terabox://, play store)
                                    if (requestUrl.startsWith("intent://") || requestUrl.startsWith("terabox://") || 
                                        requestUrl.startsWith("dubox://") || requestUrl.contains("play.google.com")) {
                                        return true 
                                    }
                                    return false
                                }
                            }

                            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                                status = TeraBoxStatus.DOWNLOADING
                                onDownloadStarted()
                                
                                try {
                                    val request = DownloadManager.Request(Uri.parse(downloadUrl))
                                    request.setMimeType(mimetype)
                                    request.addRequestHeader("User-Agent", userAgent)
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    
                                    val fileName = try {
                                        if (contentDisposition != null) {
                                            var name = URLDecoder.decode(contentDisposition, "UTF-8")
                                                .replaceFirst("attachment; filename=", "")
                                                .replace("\"", "")
                                            if (name.contains(";")) {
                                                name = name.split(";")[0].trim()
                                            }
                                            name
                                        } else ""
                                    } catch (e: Exception) { "" }.let { 
                                        if (it.isBlank()) "minecraft_pack_${System.currentTimeMillis()}.mcpack" else it 
                                    }
                                    
                                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                    
                                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    val downloadId = dm.enqueue(request)
                                    
                                    Toast.makeText(context, "Download iniciado: $fileName", Toast.LENGTH_SHORT).show()
                                    
                                    // Monitor download completion
                                    Thread {
                                        var downloading = true
                                        while (downloading) {
                                            val query = DownloadManager.Query().setFilterById(downloadId)
                                            val cursor = dm.query(query)
                                            if (cursor != null && cursor.moveToFirst()) {
                                                val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                                if (statusCol != -1) {
                                                    val statusVal = cursor.getInt(statusCol)
                                                    if (statusVal == DownloadManager.STATUS_SUCCESSFUL) {
                                                        downloading = false
                                                        val uriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                                        if (uriCol != -1) {
                                                            val localUri = cursor.getString(uriCol)
                                                            val path = Uri.parse(localUri).path
                                                            if (path != null) {
                                                                val file = File(path)
                                                                (context as? android.app.Activity)?.runOnUiThread {
                                                                    onDownloadCompleted(file)
                                                                }
                                                            }
                                                        }
                                                    } else if (statusVal == DownloadManager.STATUS_FAILED) {
                                                        downloading = false
                                                    }
                                                }
                                            }
                                            cursor?.close()
                                            Thread.sleep(1500)
                                        }
                                    }.start()

                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao baixar: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }

                            loadUrl(url)
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Native Compose Overlays for Instruction
                androidx.compose.animation.AnimatedVisibility(
                    visible = showOverlay && (status == TeraBoxStatus.WAITING_LOGIN || status == TeraBoxStatus.READY_DOWNLOAD || status == TeraBoxStatus.DOWNLOADING),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp).zIndex(10f)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().border(2.dp, NcGreenNeon, RoundedCornerShape(20.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(60.dp).background(NcGreenNeon.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(status) {
                                        TeraBoxStatus.WAITING_LOGIN -> Icons.Default.AccountCircle
                                        TeraBoxStatus.READY_DOWNLOAD -> Icons.Default.FileDownload
                                        else -> Icons.Default.HourglassEmpty
                                    },
                                    contentDescription = null,
                                    tint = NcGreenNeon,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                text = when(status) {
                                    TeraBoxStatus.WAITING_LOGIN -> "PASSO 1: LOGIN"
                                    TeraBoxStatus.READY_DOWNLOAD -> "PASSO 2: DOWNLOAD"
                                    else -> "AGUARDE..."
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = NcGreenNeon,
                                fontWeight = FontWeight.Black
                            )
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Text(
                                text = when(status) {
                                    TeraBoxStatus.WAITING_LOGIN -> "Clique no botão 'Google' destacado em verde na página para liberar o arquivo."
                                    TeraBoxStatus.READY_DOWNLOAD -> "O arquivo está pronto! Use o botão 'BAIXAR AGORA' ou o botão verde na página."
                                    TeraBoxStatus.DOWNLOADING -> "Baixando arquivo... Não feche o app."
                                    else -> ""
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            
                            Spacer(Modifier.height(20.dp))
                            
                            Button(
                                onClick = { showOverlay = false },
                                colors = ButtonDefaults.buttonColors(containerColor = NcGreenNeon),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ENTENDI", color = NcBlackDeep, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (status == TeraBoxStatus.LOADING) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = NcGreenNeon,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}

@Composable
fun AdBannerPlaceholder() {
    val context = LocalContext.current
    val adView = remember {
        com.google.android.gms.ads.AdView(context).apply {
            setAdSize(com.google.android.gms.ads.AdSize.BANNER)
            adUnitId = com.ncmine.importmine.util.AdMobManager.getBannerAdUnitId()
            loadAd(com.ncmine.importmine.util.AdMobManager.createAdRequest())
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().height(50.dp).background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { adView }
        )
    }
}
