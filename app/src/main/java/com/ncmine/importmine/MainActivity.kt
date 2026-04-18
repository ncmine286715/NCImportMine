package com.ncmine.importmine

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdMobManager.initialize(this)

        setContent {
            NCMineTheme {
                NcApp(viewModel = viewModel)
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
fun NcApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

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
            }
        }
    }
}
