package com.ncmine.importmine.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.presentation.ui.components.AddonCard
import com.ncmine.importmine.presentation.viewmodel.MainViewModel
import com.ncmine.importmine.ui.theme.*

/**
 * Tela inicial do aplicativo com dashboard e lista de addons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAddonClick: (MinecraftPack) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "NC IMPORT MINE",
                        color = NcGreenPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NcBlackDeep
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.scanAddons() },
                containerColor = NcGreenPrimary,
                contentColor = Color.Black
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Escanear")
            }
        },
        containerColor = NcBlackDeep
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Dashboard Header
            Text(
                text = "Dashboard",
                color = NcTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Seção de Addons Detectados
            Text(
                text = "Addons Detectados",
                color = NcTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NcGreenPrimary)
                }
            } else if (uiState.packs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nenhum addon encontrado.\nToque no botão para escanear.",
                        color = NcTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.packs) { pack ->
                        AddonCard(
                            pack = pack,
                            onClick = { onAddonClick(pack) },
                            onImportClick = { viewModel.importPack(pack) }
                        )
                    }
                }
            }
        }
    }
}
