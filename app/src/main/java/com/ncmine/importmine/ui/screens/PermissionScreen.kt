package com.ncmine.importmine.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.ncmine.importmine.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val needsManagePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasManageStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else true
        )
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasManageStoragePermission = Environment.isExternalStorageManager()
            if (hasManageStoragePermission) onPermissionGranted()
        }
    }

    LaunchedEffect(storagePermission.status, hasManageStoragePermission) {
        val hasBasicPermission = storagePermission.status.isGranted
        if (needsManagePermission && hasManageStoragePermission) {
            onPermissionGranted()
        } else if (!needsManagePermission && hasBasicPermission) {
            onPermissionGranted()
        }
    }

    // Animação de escala para o ícone
    val infiniteTransition = rememberInfiniteTransition(label = "iconScale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NcBlackDeep, Color(0xFF1A1A1A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícone Modernizado com Glow
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .shadow(32.dp, CircleShape, spotColor = NcGreenNeon)
                    .clip(CircleShape)
                    .background(NcGreenNeonDark.copy(alpha = 0.3f))
                    .border(3.dp, NcGreenNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = NcGreenNeon,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Acesso Necessário",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = NcTextPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Para importar seus addons para o Minecraft",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = NcGreenNeon,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Lista de permissões com visual de "Cards" moderno
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionFeatureRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "Importação Automática",
                    desc = "Movemos os arquivos .mcpack diretamente para a pasta do jogo."
                )
                PermissionFeatureRow(
                    icon = Icons.Default.Security,
                    title = "Segurança Total",
                    desc = "Seus arquivos nunca saem do celular. Tudo é offline."
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (needsManagePermission) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NcWarning.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NcWarning.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Ative 'Permitir acesso a todos os arquivos' para o NC Import Mine na próxima tela.",
                            style = MaterialTheme.typography.bodySmall.copy(color = NcTextPrimary),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    if (needsManagePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        manageStorageLauncher.launch(intent)
                    } else {
                        storagePermission.launchPermissionRequest()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = NcGreenNeon),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NcGreenNeon,
                    contentColor = NcBlackDeep
                )
            ) {
                Text(
                    text = "CONFIGURAR AGORA",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Sem esta permissão o app não funciona.",
                style = MaterialTheme.typography.labelSmall.copy(color = NcTextSecondary)
            )
        }
    }
}

@Composable
fun PermissionFeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NcBlackCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NcBlackSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = NcGreenNeon, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = NcTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(color = NcTextSecondary)
            )
        }
    }
}
