package com.ncmine.importmine

import android.content.Intent
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncmine.importmine.model.MinecraftPack
import com.ncmine.importmine.service.AdKeepAliveService
import com.ncmine.importmine.ui.screens.*
import com.ncmine.importmine.ui.theme.NCMineTheme
import com.ncmine.importmine.ui.theme.NcBlackDeep
import com.ncmine.importmine.util.AdMobManager
import com.ncmine.importmine.viewmodel.MainViewModel

sealed class Screen {
    object Home : Screen()
    data class Details(val pack: MinecraftPack) : Screen()
    data class Legal(val url: String, val title: String) : Screen()
    data class Browser(val url: String) : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdMobManager.initialize(this)

        // Verifica se o app foi aberto por um link
        val startScreen = intent?.data?.let { uri ->
            if (uri.scheme == "https" || uri.scheme == "http") {
                Screen.Browser(uri.toString())
            } else null
        } ?: Screen.Home

        setContent {
            NCMineTheme {
                RequestPermissions {
                    NcApp(viewModel = viewModel, initialScreen = startScreen)
                }
            }
        }
    }

    @Composable
    private fun RequestPermissions(content: @Composable () -> Unit) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var hasPermission by remember {
            mutableStateOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            )
        }

        val storagePermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
        }

        val manageStorageLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hasPermission = Environment.isExternalStorageManager()
            }
        }

        LaunchedEffect(Unit) {
            if (!hasPermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        manageStorageLauncher.launch(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageStorageLauncher.launch(intent)
                    }
                } else {
                    storagePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }

        if (hasPermission) {
            content()
        } else {
            // Tela de fallback ou apenas aguarda a permissão
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NcBlackDeep),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "Aguardando permissão de armazenamento...",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Para o serviço se o usuário voltar antes de 1 minuto
        stopService(Intent(this, AdKeepAliveService::class.java))
        AdMobManager.loadRewardedAd(this)
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
                        onLegalClick = { url, title -> currentScreen = Screen.Legal(url, title) }
                    )
                }
                is Screen.Details -> {
                    DetailsScreen(
                        pack = screen.pack,
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
                        onFileDownloaded = {
                            viewModel.refreshPacks() // Atualiza a lista para detectar o novo arquivo
                            currentScreen = Screen.Home
                        }
                    )
                }
            }
        }
    }
}
