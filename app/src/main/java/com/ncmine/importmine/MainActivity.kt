package com.ncmine.importmine

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncmine.importmine.model.MinecraftPack
import com.ncmine.importmine.service.AdKeepAliveService
import com.ncmine.importmine.ui.screens.*
import com.ncmine.importmine.ui.theme.*
import com.ncmine.importmine.util.AdMobManager
import com.ncmine.importmine.viewmodel.MainViewModel
import java.io.File

sealed class Screen {
    object Home : Screen()
    data class Details(val pack: MinecraftPack) : Screen()
    data class Legal(val url: String, val title: String) : Screen()
    data class Browser(val url: String) : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != -1L) {
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = dm.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusCol != -1 && cursor.getInt(statusCol) == DownloadManager.STATUS_SUCCESSFUL) {
                        val uriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        if (uriCol != -1) {
                            val localUri = cursor.getString(uriCol)
                            val path = Uri.parse(localUri).path
                            if (path != null) {
                                viewModel.onDownloadCompleted(File(path))
                            }
                        }
                    }
                }
                cursor?.close()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdMobManager.initialize(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        // Verifica se o app foi aberto por um link
        val startScreen = intent?.data?.let { uri ->
            if (uri.scheme == "https" || uri.scheme == "http") {
                Screen.Browser(uri.toString())
            } else null
        } ?: Screen.Home

        setContent {
            NCMineTheme {
                var permissionGranted by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Environment.isExternalStorageManager()
                        } else {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.READ_EXTERNAL_STORAGE
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        }
                    )
                }

                if (permissionGranted) {
                    NcApp(viewModel = viewModel, initialScreen = startScreen)
                } else {
                    PermissionScreen(
                        onPermissionGranted = {
                            permissionGranted = true
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
    }

    override fun onResume() {
        super.onResume()
        // Para o serviço se o usuário voltar antes de 1 minuto
        stopService(Intent(this, AdKeepAliveService::class.java))
        AdMobManager.loadRewardedAd(this)
        viewModel.checkMinecraftInstallation()
    }

    override fun onStop() {
        super.onStop()
        // Inicia o serviço quando o app vai para segundo plano
        startService(Intent(this, AdKeepAliveService::class.java))
    }
}

@Composable
fun NcApp(viewModel: MainViewModel, initialScreen: Screen = Screen.Home) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
    var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }

    LaunchedEffect(uiState.showImportSuccess) {
        if (uiState.showImportSuccess) {
            viewModel.dismissImportSuccess()
        }
    }

    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NcBlackDeep)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                is Screen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onImportClick = { currentScreen = Screen.Details(it) },
                        onLegalClick = { url, title -> currentScreen = Screen.Legal(url, title) },
                        onUrlImport = { currentScreen = Screen.Browser(it) }
                    )
                }
                is Screen.Details -> {
                    DetailsScreen(
                        pack = screen.pack,
                        isMinecraftInstalled = uiState.isMinecraftInstalled,
                        onBack = { currentScreen = Screen.Home },
                        onImportClick = { 
                            viewModel.importPack(it)
                        }
                    )
                }
                is Screen.Legal -> {
                    LegalScreen(
                        url = screen.url,
                        title = screen.title,
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                is Screen.Browser -> {
                    BrowserScreen(
                        url = screen.url,
                        onBack = { currentScreen = Screen.Home },
                        onDownloadCompleted = { file ->
                            viewModel.onDownloadCompleted(file)
                            currentScreen = Screen.Home
                        },
                        onDownloadStarted = {
                            viewModel.setDownloadInProgress(true)
                        }
                    )
                }
            }
        }

        // Dialog de Importação Pós-Download
        if (uiState.showDownloadImportDialog && uiState.downloadedPack != null) {
            ImportDownloadDialog(
                pack = uiState.downloadedPack!!,
                onDismiss = { viewModel.dismissDownloadImportDialog() },
                onImport = {
                    viewModel.importPack(uiState.downloadedPack!!)
                    viewModel.dismissDownloadImportDialog()
                }
            )
        }
    }
}

@Composable
fun ImportDownloadDialog(
    pack: MinecraftPack,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NcBlackCard,
        titleContentColor = NcGreenNeon,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = null,
                    tint = NcGreenNeon,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text("Download Concluído!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "O addon '${pack.name}' foi baixado com sucesso. Deseja importá-lo para o Minecraft agora?",
                    color = NcTextPrimary
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NcBlackSurface, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(pack.packTypeBadgeColor).copy(0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (pack.packType) {
                                com.ncmine.importmine.model.PackType.RESOURCE_PACK -> "🎨"
                                com.ncmine.importmine.model.PackType.BEHAVIOR_PACK -> "⚙️"
                                com.ncmine.importmine.model.PackType.WORLD_TEMPLATE -> "🌍"
                                com.ncmine.importmine.model.PackType.SKIN_PACK -> "👤"
                                com.ncmine.importmine.model.PackType.ADDON -> "🧱"
                                else -> "📦"
                            },
                            fontSize = 20.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(pack.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(pack.packTypeLabel, color = Color(pack.packTypeBadgeColor), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                colors = ButtonDefaults.buttonColors(containerColor = NcGreenNeon)
            ) {
                Text("IMPORTAR AGORA", color = NcBlackDeep, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("MAIS TARDE", color = NcTextSecondary)
            }
        }
    )
}
