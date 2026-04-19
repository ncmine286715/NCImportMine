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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
    val needsManagePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val storagePermission = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NcBlackDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícone animado
            val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(20.dp, RoundedCornerShape(32.dp), spotColor = NcGreenNeon)
                    .clip(RoundedCornerShape(32.dp))
                    .background(NcGreenNeonDark)
                    .border(2.dp, NcGreenNeon, RoundedCornerShape(32.dp)),
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
                text = "Acesso ao\nArmazenamento",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = NcTextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 42.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Precisamos de permissão para encontrar e converter seus addons do Minecraft.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = NcTextSecondary,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Explicação clara
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NcBlackCard),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, NcGreenNeon.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    PermissionItem(
                        emoji = "📁",
                        title = "Encontrar Addons",
                        description = "Escaneia sua pasta Downloads por arquivos .mcpack e .zip"
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    PermissionItem(
                        emoji = "💾",
                        title = "Backups Seguros",
                        description = "Cria cópias de segurança antes de converter arquivos"
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    PermissionItem(
                        emoji = "🔄",
                        title = "Conversão Inteligente",
                        description = "Transforma arquivos ZIP em formatos que o Minecraft entende"
                    )
                }
            }

            if (needsManagePermission) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NcWarning.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, NcWarning.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Atenção Usuário Android 11+",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = NcWarning, fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Ative \"Permitir gerenciamento de todos os arquivos\" para o NC Import Mine na próxima tela.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NcTextSecondary,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Botão principal
            Button(
                onClick = {
                    if (needsManagePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
                        storagePermission.launchPermissionRequest()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = NcGreenNeon),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NcGreenNeon,
                    contentColor = NcBlackDeep
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "CONCEDER ACESSO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "🔒 Privacidade Garantida\nSeus arquivos nunca saem do seu dispositivo.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NcTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

@Composable
private fun PermissionItem(
    emoji: String,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            modifier = Modifier.size(40.dp),
            color = NcBlackSurface,
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = emoji, fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = NcTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NcTextSecondary,
                    lineHeight = 16.sp
                )
            )
        }
    }
}
