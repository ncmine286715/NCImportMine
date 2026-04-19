package com.ncmine.importmine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncmine.importmine.presentation.viewmodel.MainViewModel
import com.ncmine.importmine.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLegalClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearFavoritesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Seção Premium
        SettingsSection(title = "Assinatura") {
            PremiumCard(
                isPremium = uiState.isPremium,
                onUpgradeClick = { viewModel.togglePremium() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seção Gerenciamento
        SettingsSection(title = "Gerenciamento") {
            SettingsItem(
                icon = Icons.Default.History,
                title = "Limpar Histórico",
                subtitle = "Remove todos os registros de importação",
                onClick = { showClearHistoryDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.FavoriteBorder,
                title = "Limpar Favoritos",
                subtitle = "Remove todos os itens favoritados",
                onClick = { showClearFavoritesDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Refresh,
                title = "Resetar Scanner",
                subtitle = "Limpa o cache e faz um novo scan completo",
                onClick = { viewModel.resetScan() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seção Sobre
        SettingsSection(title = "Sobre") {
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "Ajuda e Suporte",
                subtitle = "Dúvidas frequentes e contato",
                onClick = { onLegalClick("https://ncmine.com/help", "Ajuda") }
            )
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Termos de Uso",
                onClick = { onLegalClick("https://ncmine.com/terms", "Termos de Uso") }
            )
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "Política de Privacidade",
                onClick = { onLegalClick("https://ncmine.com/privacy", "Privacidade") }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "Versão 2.0.0 (Clean Architecture)",
            style = MaterialTheme.typography.labelSmall,
            color = NcTextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Dialogs
    if (showClearHistoryDialog) {
        NcAlertDialog(
            title = "Limpar Histórico?",
            message = "Isso removerá permanentemente o registro de todos os addons que você já importou.",
            onConfirm = {
                viewModel.clearHistory()
                showClearHistoryDialog = false
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }

    if (showClearFavoritesDialog) {
        NcAlertDialog(
            title = "Limpar Favoritos?",
            message = "Isso removerá todos os addons da sua lista de favoritos.",
            onConfirm = {
                viewModel.clearFavorites()
                showClearFavoritesDialog = false
            },
            onDismiss = { showClearFavoritesDialog = false }
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = NcGreenNeon,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = NcBlackCard),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NcBlackBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content) {
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            color = NcBlackSurface,
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = NcGreenNeon, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NcTextPrimary
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NcTextSecondary
                    )
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = NcTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun PremiumCard(isPremium: Boolean, onUpgradeClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPremium) NcGreenNeon.copy(alpha = 0.1f) 
                else Color.Transparent
            )
            .clickable(onClick = onUpgradeClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = if (isPremium) NcGreenNeon else NcBlackSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPremium) Icons.Default.Verified else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isPremium) NcBlackDeep else NcGreenNeon
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) "Modo Premium Ativo" else "Seja Premium",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isPremium) NcGreenNeon else NcTextPrimary
                    )
                )
                Text(
                    text = if (isPremium) "Aproveite o app sem anúncios" else "Remova anúncios e apoie o projeto",
                    style = MaterialTheme.typography.bodySmall,
                    color = NcTextSecondary
                )
            }
        }
    }
}

@Composable
fun NcAlertDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NcBlackCard,
        titleContentColor = Color.White,
        textContentColor = NcTextSecondary,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("CONFIRMAR", color = NcGreenNeon, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = NcTextMuted)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
