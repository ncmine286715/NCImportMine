package com.ncmine.importmine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.painterResource
import com.ncmine.importmine.R
import com.ncmine.importmine.model.MinecraftPack
import com.ncmine.importmine.model.PackType
import com.ncmine.importmine.ui.theme.*

import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.ncmine.importmine.util.AdMobManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    pack: MinecraftPack,
    isMinecraftInstalled: Boolean = true,
    onBack: () -> Unit,
    onImportClick: (MinecraftPack) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isImporting = pack.isImporting

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Add-on", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = NcGreenNeon)
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Imagem Grande
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NcBlackSurface),
                contentAlignment = Alignment.Center
            ) {
                if (pack.iconFile != null && pack.iconFile.exists()) {
                    AsyncImage(
                        model = pack.iconFile,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_emerald),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        alpha = 0.5f
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nome e Meta
            Text(
                text = pack.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = Color(pack.packTypeBadgeColor).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(pack.packTypeBadgeColor).copy(alpha = 0.5f))
                ) {
                    Text(
                        pack.packTypeLabel, 
                        color = Color(pack.packTypeBadgeColor), 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Surface(
                    color = NcBlackSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NcBlackBorder)
                ) {
                    Text(
                        pack.fileSizeFormatted, 
                        color = NcTextSecondary, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NcBlackCard),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, NcBlackBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    DetailRow(label = "Versão", value = pack.version.ifBlank { "1.0.0" })
                    DetailRow(label = "Autor", value = pack.author.ifBlank { "Desconhecido" })
                    DetailRow(label = "Formato Original", value = ".${pack.originalExtension}")
                    DetailRow(label = "Caminho", value = pack.originalFile.parentFile?.name ?: "Downloads", isPath = true)
                    
                    if (pack.manifestCount > 0) {
                        DetailRow(label = "Manifestos", value = pack.manifestCount.toString())
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Descrição", color = NcGreenNeon, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pack.description.ifBlank { "Sem descrição disponível para este pacote." },
                        color = NcTextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Botão Principal (SEMPRE COM AD REWARDED PARA IMPORTAR)
            val isZip = pack.originalExtension == "zip"
            
            if (!isMinecraftInstalled) {
                Surface(
                    color = NcError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NcError.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = NcError)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Minecraft não detectado. Instale-o para importar addons.",
                            color = NcError,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                onClick = { 
                    if (activity != null) {
                        // Exibe o vídeo premiado antes de QUALQUER importação
                        AdMobManager.showRewardedAd(activity) {
                            onImportClick(pack)
                        }
                    } else {
                        onImportClick(pack)
                    }
                },
                enabled = !isImporting && isMinecraftInstalled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(if (isMinecraftInstalled) 12.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = NcGreenNeon),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMinecraftInstalled) NcGreenNeon else NcBlackSurface,
                    disabledContainerColor = if (isMinecraftInstalled) NcGreenNeon.copy(alpha = 0.5f) else NcBlackSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NcBlackDeep, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("IMPORTANDO...", fontWeight = FontWeight.Black, color = NcBlackDeep)
                } else {
                    Icon(
                        imageVector = if (isZip) Icons.Default.Transform else Icons.Default.Gamepad, 
                        contentDescription = null, 
                        tint = if (isMinecraftInstalled) NcBlackDeep else NcTextMuted
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (!isMinecraftInstalled) "MINECRAFT NÃO ENCONTRADO"
                               else if (isZip) "CONVERTER E IMPORTAR" 
                               else "ASSISTIR E IMPORTAR", 
                        fontWeight = FontWeight.Black, 
                        color = if (isMinecraftInstalled) NcBlackDeep else NcTextMuted,
                        fontSize = 14.sp
                    )
                }
            }
            
            if (isMinecraftInstalled) {
                Text(
                    "Assista um vídeo curto para liberar a importação",
                    color = NcTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Botão Abrir Pasta
            val context = androidx.compose.ui.platform.LocalContext.current
            OutlinedButton(
                onClick = { 
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                            setDataAndType(android.net.Uri.fromFile(pack.originalFile.parentFile ?: pack.originalFile), "*/*")
                            addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Abrir pasta"))
                    } catch (e: Exception) {
                        // Fallback
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(1.5.dp, NcGreenNeon.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = NcGreenNeon)
                Spacer(modifier = Modifier.width(12.dp))
                Text("ABRIR PASTA DO ARQUIVO", color = NcGreenNeon, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isPath: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = NcTextMuted, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value, 
            color = if (isPath) NcGreenNeon else NcTextPrimary, 
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, false).padding(start = 16.dp)
        )
    }
}
