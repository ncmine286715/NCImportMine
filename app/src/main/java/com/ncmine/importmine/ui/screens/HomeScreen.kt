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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.res.painterResource
import com.ncmine.importmine.R
import com.ncmine.importmine.model.MinecraftPack
import com.ncmine.importmine.model.PackStatus
import com.ncmine.importmine.model.PackType
import com.ncmine.importmine.ui.theme.*
import com.ncmine.importmine.viewmodel.MainViewModel

/**
 * Tela principal do app com animações e cards premium
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onImportClick: (MinecraftPack) -> Unit,
    onLegalClick: (String, String) -> Unit,
    onUrlImport: (String) -> Unit
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
            // Header com logo
            NcHeader(onLegalClick, onUrlImport)

            // Tabs de Navegação
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

            // Conteúdo principal
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
                    uiState.selectedTab == 0 -> {
                        EmptyState(
                            hasScanned = uiState.scanCompleted,
                            onScanClick = { viewModel.startFastScan() },
                            onLegalClick = onLegalClick
                        )
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (uiState.selectedTab == 1) "❤️" else "📜", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (uiState.selectedTab == 1) "Nenhum favorito" else "Histórico vazio",
                                    color = NcTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (uiState.selectedTab == 1) "Seus addons favoritos aparecerão aqui." else "Seus arquivos importados aparecerão aqui.",
                                    color = NcTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Banner AdMob no rodapé
            AdBannerFooter()
        }

        // Snackbars de erro/sucesso
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Mensagem de erro
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

            // Mensagem de sucesso
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

// ============================================================
// HEADER
// ============================================================

@Composable
fun NcHeader(
    onLegalClick: (String, String) -> Unit,
    onUrlImport: (String) -> Unit
) {
    var showAbout by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlToImport by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D0D), NcBlackDeep)
                )
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo Emerald Nova
                Image(
                    painter = painterResource(id = R.drawable.app_logo_emerald),
                    contentDescription = "Logo NC MINE",
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = NcGreenNeon)
                        .background(NcBlackSurface, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                )
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column {
                    Text(
                        text = "NCMINE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "ADDON FINDER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NcGreenNeon,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                }
            }

            Row {
                IconButton(
                    onClick = { showUrlDialog = true },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(CircleShape)
                        .background(NcBlackSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Importar via Link",
                        tint = NcGreenNeon
                    )
                }

                IconButton(
                    onClick = { showAbout = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NcBlackSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações",
                        tint = NcGreenNeon
                    )
                }
            }
        }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            containerColor = NcBlackCard,
            title = { Text("Importar via Link", color = NcGreenNeon) },
            text = {
                Column {
                    Text("Cole um link do TeraBox ou direto para o arquivo:", color = NcTextSecondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = urlToImport,
                        onValueChange = { urlToImport = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://terabox.com/...", color = NcTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NcGreenNeon,
                            unfocusedBorderColor = NcBlackBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (urlToImport.isNotBlank()) {
                            onUrlImport(urlToImport)
                            showUrlDialog = false
                            urlToImport = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NcGreenNeon)
                ) {
                    Text("ABRIR", color = NcBlackDeep)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("CANCELAR", color = NcTextSecondary)
                }
            }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false }, onLegalClick = onLegalClick)
    }

    // Divider com brilho neon
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        NcGreenNeon.copy(alpha = 0.6f),
                        NcGreenNeon,
                        NcGreenNeon.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun NanoLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = NcGreenNeon)
            .background(NcBlackSurface, RoundedCornerShape(12.dp))
            .border(1.5.dp, NcGreenNeon, RoundedCornerShape(12.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = null,
            tint = NcGreenNeon,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
        )
        Text(
            text = "N",
            color = NcBlackDeep,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit, onLegalClick: (String, String) -> Unit) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NcBlackCard,
        titleContentColor = NcGreenNeon,
        textContentColor = NcTextSecondary,
        title = { Text("Sobre o NCMINE Addon Finder", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Versão 1.1.0 - Pro Engine", color = NcTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("O importador definitivo de Add-ons, mundos e pacotes para Minecraft Bedrock Edition.")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Recursos:", fontWeight = FontWeight.Bold, color = NcTextPrimary)
                Text("• Suporte total a .mcpack, .mcworld e .mcaddon")
                Text("• Conversão automática de .zip")
                Text("• Backups automáticos de segurança")
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Publicador:", fontWeight = FontWeight.Bold, color = NcTextPrimary)
                Text("NC MINE", color = NcGreenNeon)
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        onDismiss()
                        onLegalClick("https://mineaddonsnews.online/politicadeprivacidade", "Política de Privacidade")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NcBlackSurface)
                ) {
                    Text("Política de Privacidade", color = NcTextSecondary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { 
                        onDismiss()
                        onLegalClick("https://mineaddonsnews.online/termosapp", "Termos de Uso")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NcBlackSurface)
                ) {
                    Text("Termos de Uso", color = NcTextSecondary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { 
                        onDismiss()
                        onLegalClick("https://mineaddonsnews.online/concetimento", "Consentimento de Dados")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NcBlackSurface)
                ) {
                    Text("Consentimento de Dados", color = NcTextSecondary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("FECHAR", color = NcGreenNeon)
            }
        }
    )
}

// ============================================================
// TELA DE CONFIGURAÇÕES
// ============================================================

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLegalClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Seção: Geral
        SettingsCard(title = "Geral") {
            SettingsItem(
                icon = Icons.Default.Folder,
                title = "Pasta padrão",
                subtitle = "Downloads/",
                onClick = { /* Implementar seletor se necessário */ }
            )
        }

        // Seção: Notificações
        SettingsCard(title = "Notificações") {
            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                title = "Ao detectar addon",
                subtitle = "Avisar quando um novo arquivo for encontrado",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )
        }

        // Seção: Instalação Rápida
        SettingsCard(title = "Instalação Rápida") {
            SettingsItem(
                icon = Icons.Default.Bolt,
                title = "Recompensado • Ativo",
                subtitle = "Instalação otimizada ativa",
                onClick = { }
            )
        }

        // Seção: Sobre
        SettingsCard(title = "Sobre") {
            SettingsItem(
                icon = Icons.Default.Update,
                title = "Verificar atualização",
                subtitle = "v1.0.0 · Atualizado",
                onClick = { }
            )
            SettingsItem(
                icon = Icons.Default.Language,
                title = "Site NCMine",
                subtitle = "https://linktr.ee/ncgtz",
                onClick = { 
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ncmine.com.br")))
                    } catch (e: Exception) {}
                }
            )
            SettingsItem(
                icon = Icons.Default.Description,
                title = "Política de privacidade",
                subtitle = "Ver documento",
                onClick = { onLegalClick("https://mineaddonsnews.online/politicadeprivacidade", "Privacidade") }
            )
        }

        // Card de Manutenção (Antigas opções)
        SettingsCard(title = "Manutenção") {
            SettingsItem(
                icon = Icons.Default.DeleteSweep,
                title = "Limpar Histórico",
                subtitle = "Remove a marca de 'Importado' dos arquivos",
                onClick = { viewModel.clearHistory() }
            )
            SettingsItem(
                icon = Icons.Default.RestartAlt,
                title = "Reiniciar Cache",
                subtitle = "Limpa a lista e força nova busca",
                onClick = { viewModel.resetScan() }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "NCMINE Versão 1.1.0 (Pro)",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = NcTextMuted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            title,
            color = NcGreenNeon,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = NcBlackCard),
            border = BorderStroke(1.dp, NcBlackBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(NcBlackSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NcGreenNeon, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NcTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = NcTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NcBlackBorder, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(NcBlackSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NcGreenNeon, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NcTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = NcTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NcGreenNeon,
                uncheckedThumbColor = NcTextSecondary,
                uncheckedTrackColor = NcBlackSurface
            )
        )
    }
}

// ============================================================
// ESTADO VAZIO / BOTÃO PRINCIPAL
// ============================================================

@Composable
fun EmptyState(
    hasScanned: Boolean, 
    isTabEmpty: Boolean = false, 
    onScanClick: () -> Unit,
    onLegalClick: (String, String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isTabEmpty) {
            Text("🔍", fontSize = 60.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Nada por aqui ainda",
                color = NcTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                "Importe ou favorite addons para vê-los aqui.",
                color = NcTextSecondary,
                textAlign = TextAlign.Center
            )
        } else {
            // Banner Explicativo Visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(NcGreenNeonDark.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
                    .border(1.dp, NcGreenNeon.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Ícone Central de Pasta (Novo Ícone)
                    Image(
                        painter = painterResource(id = R.drawable.ic_folder_mc),
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer {
                                scaleX = 1f + (glowAlpha * 0.1f)
                                scaleY = 1f + (glowAlpha * 0.1f)
                            }
                            .shadow(20.dp, CircleShape, spotColor = NcGreenNeon)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "IMPORTADOR AUTOMÁTICO",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = NcGreenNeon,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    )
                    
                    Text(
                        text = "Encontre seus Addons em segundos",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = NcTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Grid de Instruções Rápida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InstructionCard(
                    icon = "📥",
                    title = "Baixe",
                    desc = "Addons na web",
                    modifier = Modifier.weight(1f)
                )
                InstructionCard(
                    icon = "🔍",
                    title = "Analise",
                    desc = "Clique no botão",
                    modifier = Modifier.weight(1f)
                )
                InstructionCard(
                    icon = "🎮",
                    title = "Jogue",
                    desc = "Importe e divirta-se",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // BOTÃO PRINCIPAL GIGANTE
            Button(
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = NcGreenNeon.copy(alpha = 0.5f),
                        spotColor = NcGreenNeon.copy(alpha = 0.5f)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NcGreenNeon,
                    contentColor = NcBlackDeep
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "BUSCAR MEUS ADDONS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão Abrir Pasta de Downloads
            val context = LocalContext.current
            OutlinedButton(
                onClick = { 
                    try {
                        val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback se não suportado
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        context.startActivity(Intent.createChooser(intent, "Abrir Downloads"))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(1.5.dp, NcTextMuted),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = NcTextMuted)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ABRIR PASTA DOWNLOADS", color = NcTextMuted, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão Baixar Mais Addons
            OutlinedButton(
                onClick = { onLegalClick("https://linktr.ee/ncgtz", "Baixar Addons") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(1.5.dp, NcGreenNeon),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = NcGreenNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("BAIXAR MAIS ADDONS", color = NcGreenNeon, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Formatos suportados
            Text(
                text = "Suporte total a .mcpack, .mcworld, .mcaddon e .zip",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NcTextMuted,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
fun InstructionCard(icon: String, title: String, desc: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NcBlackCard),
        border = BorderStroke(1.dp, NcBlackBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NcTextMuted,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================
// LOADING / SCANNING
// ============================================================

@Composable
fun ScanningIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer { rotationZ = rotation },
                color = NcGreenNeon,
                strokeWidth = 3.dp,
                trackColor = NcGreenNeonDark
            )
            Text(text = "🔍", fontSize = 32.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Analisando arquivos...",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = NcGreenNeon,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Downloads • Documents • Minecraft",
            style = MaterialTheme.typography.bodyMedium.copy(color = NcTextSecondary)
        )
    }
}

// ============================================================
// LISTA DE PACOTES
// ============================================================

@Composable
fun PackList(
    packs: List<MinecraftPack>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    sortOrder: com.ncmine.importmine.viewmodel.SortOrder = com.ncmine.importmine.viewmodel.SortOrder.NEWEST,
    onSortOrderChange: (com.ncmine.importmine.viewmodel.SortOrder) -> Unit = {},
    importingPackId: String?,
    onImportClick: (MinecraftPack) -> Unit,
    onToggleFavorite: (MinecraftPack) -> Unit,
    showFilters: Boolean = true
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (showFilters) {
            // Barra de Pesquisa
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Pesquisar addons...", color = NcTextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NcGreenNeon) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar", tint = NcTextMuted)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NcGreenNeon,
                    unfocusedBorderColor = NcBlackBorder,
                    cursorColor = NcGreenNeon,
                    focusedContainerColor = NcBlackSurface,
                    unfocusedContainerColor = NcBlackSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Opções de Ordenação
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip(
                    label = "Recentes",
                    selected = sortOrder == com.ncmine.importmine.viewmodel.SortOrder.NEWEST,
                    onClick = { onSortOrderChange(com.ncmine.importmine.viewmodel.SortOrder.NEWEST) }
                )
                SortChip(
                    label = "A-Z",
                    selected = sortOrder == com.ncmine.importmine.viewmodel.SortOrder.A_Z,
                    onClick = { onSortOrderChange(com.ncmine.importmine.viewmodel.SortOrder.A_Z) }
                )
                SortChip(
                    label = "Tamanho",
                    selected = sortOrder == com.ncmine.importmine.viewmodel.SortOrder.SIZE,
                    onClick = { onSortOrderChange(com.ncmine.importmine.viewmodel.SortOrder.SIZE) }
                )
            }
        }

        // Header da lista
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (packs.isEmpty() && searchQuery.isNotEmpty()) "Nenhum resultado" else "${packs.size} pacote(s) encontrado(s)",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = NcGreenNeon,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "💾 Backup automático",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NcTextSecondary
                )
            )
        }

        if (packs.isEmpty() && searchQuery.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum addon encontrado com este nome.", color = NcTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(packs, key = { it.id }) { pack ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                    ) {
                        PackCard(
                            pack = pack,
                            isImporting = importingPackId == pack.id,
                            onImportClick = { onImportClick(pack) },
                            onToggleFavorite = { onToggleFavorite(pack) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) NcGreenNeon.copy(alpha = 0.15f) else NcBlackSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) NcGreenNeon else NcBlackBorder)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (selected) NcGreenNeon else NcTextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

// ============================================================
// CARD DE PACOTE
// ============================================================

@Composable
fun PackCard(
    pack: MinecraftPack,
    isImporting: Boolean,
    onImportClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val packColor = Color(pack.packTypeBadgeColor)
    val isImported = pack.status == PackStatus.IMPORTED || pack.isImported
    val isError = pack.status == PackStatus.ERROR

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onImportClick() }
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isImported) NcGreenNeon.copy(0.2f) else packColor.copy(0.1f),
                spotColor = if (isImported) NcGreenNeon.copy(0.2f) else packColor.copy(0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NcBlackCard
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    packColor.copy(alpha = 0.5f),
                    NcBlackBorder,
                    packColor.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do pacote
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NcBlackSurface)
                    .border(1.dp, packColor.copy(0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (pack.iconFile != null && pack.iconFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(pack.iconFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Ícone do pacote",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Emoji fallback baseado no tipo
                    Text(
                        text = when (pack.packType) {
                            PackType.RESOURCE_PACK -> "🎨"
                            PackType.BEHAVIOR_PACK -> "⚙️"
                            PackType.WORLD_TEMPLATE -> "🌍"
                            PackType.SKIN_PACK -> "👤"
                            PackType.ADDON -> "🧱"
                            PackType.UNKNOWN -> "📦"
                        },
                        fontSize = 28.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Informações do pacote
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Nome
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = NcTextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (pack.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (pack.isFavorite) NcGreenNeon else NcTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Descrição
                if (pack.description.isNotBlank()) {
                    Text(
                        text = pack.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NcTextSecondary
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Badges: tipo + versão + tamanho
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge tipo
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(packColor.copy(alpha = 0.15f))
                            .border(0.5.dp, packColor.copy(0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = pack.packTypeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = packColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Badge versão
                    if (pack.version.isNotBlank() && pack.version != "1.0.0") {
                        Text(
                            text = "v${pack.version}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NcTextMuted
                            )
                        )
                    }

                    // Tamanho
                    Text(
                        text = pack.fileSizeFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NcTextMuted
                        )
                    )

                    // Badge ZIP → MCPACK
                    if (pack.originalFile.extension.lowercase() == "zip") {
                        val label = if (pack.manifestCount > 1) "ZIP→ADDON" else "ZIP→MCPACK"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NcWarning.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NcWarning,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                // Autor
                if (pack.author.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "por ${pack.author}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NcTextMuted,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Botão de importar
            when {
                isImporting -> {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = NcGreenNeon,
                            strokeWidth = 2.5.dp
                        )
                    }
                }

                isImported -> {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NcGreenNeonDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Importado",
                            tint = NcGreenNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                isError -> {
                    IconButton(
                        onClick = onImportClick,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NcError.copy(0.15f))
                            .border(1.dp, NcError.copy(0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Tentar novamente",
                            tint = NcError,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                else -> {
                    // Botão IMPORTAR principal
                    FilledTonalButton(
                        onClick = onImportClick,
                        modifier = Modifier
                            .height(48.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = NcGreenNeon.copy(0.3f),
                                spotColor = NcGreenNeon.copy(0.3f)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NcGreenNeon,
                            contentColor = NcBlackDeep
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "IMPORTAR",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// SNACKBAR PERSONALIZADO
// ============================================================

@Composable
fun NcSnackbar(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val color = if (isError) NcError else NcGreenNeon
    val icon = if (isError) Icons.Default.Error else Icons.Default.CheckCircle

    LaunchedEffect(message) {
        kotlinx.coroutines.delay(4000)
        onDismiss()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 80.dp), // Acima do banner
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = NcBlackCard
        ),
        border = BorderStroke(1.dp, color.copy(0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NcTextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = NcTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ============================================================
// BANNER ADMOB NO RODAPÉ / NATIVE ADS
// ============================================================

@Composable
fun AdBannerFooter() {
    val context = LocalContext.current
    val adView = remember {
        com.google.android.gms.ads.AdView(context).apply {
            setAdSize(com.google.android.gms.ads.AdSize.BANNER)
            adUnitId = com.ncmine.importmine.util.AdMobManager.getBannerAdUnitId()
            loadAd(com.ncmine.importmine.util.AdMobManager.createAdRequest())
        }
    }

    Column(
        modifier = Modifier
            .background(AdBannerBackground)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NcBlackBorder)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { adView }
            )
        }
    }
}
