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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.ncmine.importmine.ui.theme.*

/**
 * Tela de solicitação de permissão de armazenamento
 * Explica claramente por que a permissão é necessária
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current

    // Para Android 11+: MANAGE_EXTERNAL_STORAGE
    // Para Android 8-10: READ/WRITE_EXTERNAL_STORAGE
    val needsManagePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    // Estado de permissão usando Accompanist
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Verifica se tem permissão total de armazenamento (Android 11+)
    var hasManageStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else true
        )
    }

    // Launcher para abrir configurações de permissão especial
    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasManageStoragePermission = Environment.isExternalStorageManager()
            if (hasManageStoragePermission) onPermissionGranted()
        }
    }

    // Verifica se já tem tudo que precisa
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
            // Ícone grande
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NcGreenNeonDark)
                    .border(2.dp, NcGreenNeon, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = NcGreenNeon,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Permissão de\nArmazenamento",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = NcTextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Explicação clara
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NcBlackCard),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, NcGreenNeon.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    PermissionItem(
                        emoji = "📁",
                        title = "Acessar pasta Downloads",
                        description = "Para encontrar seus arquivos .mcpack e .mcworld"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PermissionItem(
                        emoji = "💾",
                        title = "Criar backups automáticos",
                        description = "Salva cópias em NC Import Mine/Backup"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PermissionItem(
                        emoji = "🔄",
                        title = "Converter arquivos ZIP",
                        description = "Salva o .mcpack convertido no seu dispositivo"
                    )
                }
            }

            if (needsManagePermission) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = NcWarning.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, NcWarning.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Android 11+ requer permissão especial",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = NcWarning, fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Na próxima tela, encontre \"NC Import Mine\" e ative \"Permitir gerenciamento de todos os arquivos\".",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NcTextSecondary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botão principal
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
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NcGreenNeon,
                    contentColor = NcBlackDeep
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Conceder Permissão",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🔒 Seus dados ficam no dispositivo.\nNenhuma informação é enviada para servidores.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NcTextMuted,
                    textAlign = TextAlign.Center
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
        Text(text = emoji, fontSize = 20.sp, modifier = Modifier.width(32.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = NcTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NcTextSecondary
                )
            )
        }
    }
}
