package com.ncmine.importmine.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.ui.theme.*

/**
 * Tela de detalhes do addon com informações completas e CTAs de ação
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    pack: MinecraftPack,
    onBack: () -> Unit,
    onImportClick: (MinecraftPack) -> Unit,
    onFixClick: (MinecraftPack) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Addon", color = NcTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = NcTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NcBlackDeep)
            )
        },
        containerColor = NcBlackDeep
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header com Ícone e Nome
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NcBlackCard),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        color = NcBlackSurface,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = NcGreenPrimary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = pack.name,
                        color = NcTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = pack.packTypeLabel,
                        color = NcGreenPrimary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Descrição
            Text("Descrição", color = NcTextSecondary, fontSize = 14.sp)
            Text(
                text = pack.description.ifEmpty { "Nenhuma descrição fornecida." },
                color = NcTextPrimary,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Informações Técnicas
            InfoRow(label = "Versão", value = pack.version)
            InfoRow(label = "Autor", value = pack.author.ifEmpty { "Desconhecido" })
            InfoRow(label = "Tamanho", value = pack.fileSizeFormatted)
            InfoRow(label = "Extensão", value = pack.originalExtension.uppercase())

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            // Botões de Ação (CTAs)
            if (pack.originalExtension == "zip") {
                Button(
                    onClick = { onFixClick(pack) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NcBlackSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Corrigir Automaticamente", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = { onImportClick(pack) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NcGreenPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("IMPORTAR AGORA", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = NcTextSecondary, fontSize = 14.sp)
        Text(value, color = NcTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
