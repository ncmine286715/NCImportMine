package com.ncmine.importmine.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.ncmine.importmine.R
import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.domain.model.PackStatus
import com.ncmine.importmine.domain.model.PackType
import com.ncmine.importmine.presentation.viewmodel.MainViewModel
import com.ncmine.importmine.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onImportClick: (MinecraftPack) -> Unit,
    onLegalClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NcBlackDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            NcHeader(onLegalClick)

            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = NcBlackDeep,
                contentColor = NcGreenNeon,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = NcGreenNeon
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("DESCOBRIR", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text("FAVORITOS", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    text = { Text("HISTÓRICO", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }

            if (!uiState.isPremium) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = {
                        AdView(it).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = viewModel.adMobManager.getBannerAdUnitId()
                            loadAd(viewModel.adMobManager.createAdRequest())
                        }
                    }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.selectedTab == 3 -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onLegalClick = onLegalClick
                        )
                    }
                    uiState.isScanning && uiState.packs.isEmpty() -> {
                        ScanningIndicator()
                    }
                    uiState.filteredPacks.isNotEmpty() || (uiState.selectedTab == 0 && uiState.searchQuery.isNotEmpty()) -> {
                        PackList(
                            packs = uiState.filteredPacks,
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            sortOrder = uiState.sortOrder,
                            onSortOrderChange = { viewModel.setSortOrder(it) },
                            importingPackId = uiState.importingPackId,
                            onImportClick = onImportClick,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            showFilters = uiState.selectedTab != 3
                        )
                    }
                    else -> {
                        EmptyState(
                            hasScanned = uiState.scanCompleted,
                            isTabEmpty = uiState.selectedTab != 0,
                            onScanClick = { viewModel.startFastScan() },
                            onLegalClick = onLegalClick
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                uiState.errorMessage?.let { msg ->
                    NcSnackbar(
                        message = msg,
                        isError = true,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.successMessage != null && uiState.errorMessage == null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                uiState.successMessage?.let { msg ->
                    NcSnackbar(
                        message = msg,
                        isError = false,
                        onDismiss = { viewModel.clearSuccess() }
                    )
                }
            }
        }
    }
}

