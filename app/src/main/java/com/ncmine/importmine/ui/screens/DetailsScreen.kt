package com.ncmine.importmine.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ncmine.importmine.R
import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.presentation.viewmodel.MainViewModel
import com.ncmine.importmine.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    pack: MinecraftPack,
    viewModel: MainViewModel,
    onBack: () -> Unit
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = NcGreenNeon)
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
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NcBlackSurface)
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
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
                        modifier = Modifier.size(140.dp),
                        alpha = 0.6f
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

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
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = Color(pack.packTypeBadgeColor).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(pack.packTypeBadgeColor).copy(alpha = 0.4f))
                ) {
                    Text(
                        pack.packTypeLabel, 
                        color = Color(pack.packTypeBadgeColor), 
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Surface(
                    color = NcBlackSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NcBlackBorder)
                ) {
                    Text(
                        pack.fileSizeFormatted, 
                        color = NcTextSecondary, 
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NcBlackCard),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NcBlackBorder)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    DetailRow(label = "Versão", value = pack.version.ifBlank { "1.0.0" })
                    DetailRow(label = "Autor", value = pack.author.ifBlank { "Desconhecido" })
                    DetailRow(label = "Formato Original", value = ".${pack.originalExtension}")
                    DetailRow(label = "Caminho", value = pack.originalFile.parentFile?.name ?: "Downloads", isPath = true)
                    
                    if (pack.manifestCount > 0) {
                        DetailRow(label = "Manifestos", value = pack.manifestCount.toString())
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Descrição", color = NcGreenNeon, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = pack.description.ifBlank { "Sem descrição disponível para este pacote." },
                        color = NcTextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Botão Principal
            val isZip = pack.originalExtension == "zip"
            
            Button(
                onClick = { 
                    if (activity != null) {
                        viewModel.importPack(activity, pack)
                    }
                },
                enabled = !isImporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = NcGreenNeon),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NcGreenNeon,
                    disabledContainerColor = NcGreenNeon.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = NcBlackDeep, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("IMPORTANDO...", fontWeight = FontWeight.Black, color = NcBlackDeep, fontSize = 16.sp)
                } else {
                    Icon(
                        imageVector = if (isZip) Icons.Default.Transform else Icons.Default.Gamepad, 
                        contentDescription = null, 
                        tint = NcBlackDeep,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (isZip) "CONVERTER E IMPORTAR" else "IMPORTAR PARA MINECRAFT", 
                        fontWeight = FontWeight.Black, 
                        color = NcBlackDeep,
                        fontSize = 16.sp
                    )
                }
            }
            
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            if (!uiState.isPremium) {
                Text(
                    "Pode ser exibido um anúncio para liberar a importação",
                    color = NcTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Botão Abrir Pasta
            OutlinedButton(
                onClick = { 
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                            val parentFile = pack.originalFile.parentFile ?: pack.originalFile
                            setDataAndType(android.net.Uri.fromFile(parentFile), "*/*")
                            addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Abrir pasta"))
                    } catch (e: Exception) {
                        // Fallback ou mensagem de erro
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                border = BorderStroke(2.dp, NcGreenNeon.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NcGreenNeon)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("ABRIR PASTA DO ARQUIVO", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isPath: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = NcTextMuted, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value, 
            color = if (isPath) NcGreenNeon else NcTextPrimary, 
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, false).padding(start = 20.dp),
            textAlign = TextAlign.End
        )
    }
}
