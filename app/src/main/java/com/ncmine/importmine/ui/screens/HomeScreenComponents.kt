package com.ncmine.importmine.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ncmine.importmine.R
import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.presentation.viewmodel.SortOrder
import com.ncmine.importmine.ui.theme.*

@Composable
fun NcHeader(onLegalClick: (String, String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.app_logo_emerald),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "NC IMPORT MINE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            )
        }
        
        IconButton(onClick = { onLegalClick("https://ncmine.com/help", "Ajuda") }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Ajuda", tint = NcGreenNeon)
        }
    }
}

@Composable
fun ScanningIndicator() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = NcGreenNeon, strokeWidth = 4.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Escaneando arquivos...",
            color = NcTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Isso pode levar alguns segundos",
            color = NcTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PackList(
    packs: List<MinecraftPack>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    importingPackId: String?,
    onImportClick: (MinecraftPack) -> Unit,
    onToggleFavorite: (MinecraftPack) -> Unit,
    showFilters: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (showFilters) {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                sortOrder = sortOrder,
                onSortOrderChange = onSortOrderChange
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(packs, key = { it.id }) { pack ->
                PackCard(
                    pack = pack,
                    isImporting = importingPackId == pack.id,
                    onImportClick = { onImportClick(pack) },
                    onToggleFavorite = { onToggleFavorite(pack) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp)),
            placeholder = { Text("Pesquisar addons...", color = NcTextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NcGreenNeon) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = NcBlackCard,
                unfocusedContainerColor = NcBlackCard,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = NcGreenNeon,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Box {
            IconButton(
                onClick = { showSortMenu = true },
                modifier = Modifier
                    .size(52.dp)
                    .background(NcBlackCard, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = NcGreenNeon)
            }
            
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                modifier = Modifier.background(NcBlackCard)
            ) {
                SortMenuItem("A-Z", sortOrder == SortOrder.A_Z) { 
                    onSortOrderChange(SortOrder.A_Z)
                    showSortMenu = false
                }
                SortMenuItem("Mais Recentes", sortOrder == SortOrder.NEWEST) { 
                    onSortOrderChange(SortOrder.NEWEST)
                    showSortMenu = false
                }
                SortMenuItem("Tamanho", sortOrder == SortOrder.SIZE) { 
                    onSortOrderChange(SortOrder.SIZE)
                    showSortMenu = false
                }
            }
        }
    }
}

@Composable
fun SortMenuItem(text: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { 
            Text(
                text, 
                color = if (selected) NcGreenNeon else Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ) 
        },
        onClick = onClick
    )
}

@Composable
fun PackCard(
    pack: MinecraftPack,
    isImporting: Boolean,
    onImportClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onImportClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NcBlackCard),
        border = BorderStroke(1.dp, if (pack.isImported) NcGreenNeon.copy(alpha = 0.3f) else NcBlackBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(14.dp))
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
                        modifier = Modifier.size(40.dp),
                        alpha = 0.5f
                    )
                }
                
                if (pack.isImported) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NcGreenNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NcGreenNeon,
                            modifier = Modifier.size(20.dp).padding(2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = pack.packTypeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(pack.packTypeBadgeColor)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pack.fileSizeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = NcTextMuted
                    )
                    if (pack.author.isNotBlank()) {
                        Text(" • ", color = NcTextMuted)
                        Text(
                            text = pack.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = NcTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Ações
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (pack.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (pack.isFavorite) NcGreenNeon else NcTextMuted
                    )
                }
                
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NcGreenNeon, strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = NcTextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    hasScanned: Boolean,
    isTabEmpty: Boolean,
    onScanClick: () -> Unit,
    onLegalClick: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            color = NcBlackCard,
            shape = CircleShape,
            border = BorderStroke(2.dp, NcGreenNeon.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isTabEmpty) Icons.Default.Inbox else Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = NcGreenNeon.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isTabEmpty) "Nada por aqui ainda" else "Nenhum addon encontrado",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = if (isTabEmpty) "Os itens que você favoritar ou importar aparecerão nesta lista." 
                   else "Certifique-se de que seus arquivos .mcpack ou .zip estão na pasta Downloads.",
            style = MaterialTheme.typography.bodyMedium,
            color = NcTextSecondary,
            textAlign = TextAlign.Center
        )
        
        if (!hasScanned && !isTabEmpty) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = NcGreenNeon),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(56.dp).fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = NcBlackDeep)
                Spacer(modifier = Modifier.width(12.dp))
                Text("BUSCAR ADDONS AGORA", fontWeight = FontWeight.Black, color = NcBlackDeep)
            }
        }
    }
}

@Composable
fun NcSnackbar(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        delay(4000)
        onDismiss()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) NcError.copy(alpha = 0.9f) else NcGreenNeon.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) Color.White else NcBlackDeep
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = if (isError) Color.White else NcBlackDeep,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = if (isError) Color.White else NcBlackDeep,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private suspend fun delay(timeMillis: Long) {
    kotlinx.coroutines.delay(timeMillis)
}
