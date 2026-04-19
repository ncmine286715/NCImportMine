package com.ncmine.importmine.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.ui.theme.*

/**
 * Card premium para exibição de addons na lista
 */
@Composable
fun AddonCard(
    pack: MinecraftPack,
    onClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(1.dp, NcBlackBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = NcBlackCard)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do Addon (ou placeholder)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NcBlackSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = NcGreenPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Informações do Addon
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.name,
                    color = NcTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge de Tipo
                    Surface(
                        color = NcGreenNeonDark,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = pack.packTypeLabel,
                            color = NcGreenPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "v${pack.version}",
                        color = NcTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Botão de Ação
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .background(NcBlackSurface, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver Detalhes",
                    tint = NcTextSecondary
                )
            }
        }
    }
}
